{ |print|
	// var print = true;
	var susSrc = Dictionary[

		// 'sine' -> { SinOsc.ar(440) },

		'thru' -> { |sig| sig.tanh },

		'freeze' -> { |sig|
			sig = FFT(LocalBuf(1024),sig);
			sig = PV_Freeze(sig,1);
			sig = IFFT(sig);
		},

		'loop' -> { |sig|
			sig = sig * Env.sine(0.4).kr;      // map to an arg??
			sig = LocalIn.ar(1) + sig;
			sig = DelayC.ar(sig,0.75,\dly.kr(0.18).linlin(0,1,0.08,0.75));
			LocalOut.ar(sig) * 0.9;
			sig
		},
	];

	var dcySrc = Dictionary[

		'oneTap' -> { |sig, env|
			DelayC.ar(sig * env,0.5,\dly.kr(0.25).linexp(0,1,0.08,0.5) )
		},

		'fbDel' -> { |sig, env|
			CombC.ar(sig * env,0.5,\dly.kr(0.25).linexp(0,1,0.08,0.5),\dcy.kr(0.4).linlin(0,1,0.5,2))
		},

		'modDel' -> { |sig, env|
			CombC.ar(sig * env,0.55,\dly.kr(0.25).linlin(0,1,0.2,0.25) *
				XLine.kr(1,Select.kr(\detuneIndex.kr(1).round(1),[0.5,2]),10),\dcy.kr(0.4).linlin(0,1,0.5,2))
		},
	];

	var mod = {
		var modFreq = \modFreq.kr(0.5).linexp(0,1,1,12);
		var synthArray = [
			SinOsc.ar(modFreq).range(0.5,1),
			LFNoise0.ar(modFreq).range(0.5,1),
			LFNoise1.ar(modFreq).range(0.5,1),
			1,
			LFSaw.ar(modFreq).range(0.5,1)
		];
		Select.kr(\modIndex.kr(0).linlin(0,1,0,synthArray.size-1).round(1),synthArray)
	};

	var post = Dictionary[

		'Ring' -> { |sig, mod|
			sig * SinOsc.ar(\freq.kr(0.5).linexp(0,1,40,600) * mod);
		},

		'FreqShift' -> { |sig, mod|
			FreqShift.ar(sig,\freqDev.kr(0.6).linlin(0,1,-150,150) * mod)
		},

		'Comb' -> { |sig, mod|
			CombC.ar(sig,0.2,\freq.kr(0).linexp(0,1,40,250).reciprocal * mod,-2);
		},

		'Clip' -> { |sig|
			(sig * 8).clip2 * -16.dbamp
		},

		'Bpf' -> { |sig, mod|
			BPF.ar(sig,\fFreq.kr(0.5).linexp(0,1,500,1200) * mod,0.4,2).clip2
		},

		'Rlpf' -> { |sig, mod|
			RLPF.ar(sig,\fFreq.kr(0.5).linexp(0,1,600,3000) * mod,0.4,2).clip2
		},

		'BitCrush' -> { |sig, mod|
			var snd = Decimator.ar(sig,SampleRate.ir * \sRate.kr(0.25).linlin(0,1,0.1,0.25) * mod,24 * \bits.kr(0).linlin(0,1,0.1,0.5) * mod );
			snd = Compander.ar(snd,snd,-18.dbamp,10,1,0.01,0.01);
			snd = Limiter.ar(snd);
		},

		'PitchShift' -> { |sig, mod|
			PitchShift.ar(sig,0.2,\freq.kr(0).linexp(0,1,0.5,2) * mod)
		}

	];

	var env = { |dAction|
		var envArray = Env([0,1,0],[\atk.kr(0.01), \rls.kr(1)]).asArray;
		envArray[6,10..] = \shape.kr(4);
		envArray[7,11..] = \curve.kr(4);
		EnvGen.kr(envArray, doneAction: dAction)
	};

	var silence = { |sig| DetectSilence.ar(sig,-42.dbamp,0.1,2) };

	// envs after processing
	susSrc.keysValuesDo({ |srcKey, srcFunc|
		post.keysValuesDo({ |postKey, postFunc|
			env.do({ |envFunc|
				var synthKey = srcKey ++ postKey;

				SynthDef(synthKey.asSymbol,{
					var sig = SynthDef.wrap(srcFunc, prependArgs: [ In.ar( \inBus.kr(0) )]);
					var lfo = SynthDef.wrap( mod );
					sig = SynthDef.wrap(postFunc, prependArgs: [ sig, lfo ]);
					sig = sig.tanh;
					sig = LeakDC.ar(sig);

					sig = sig * SynthDef.wrap(envFunc,prependArgs: [ 2 ]);
					sig = Pan2.ar(sig,\pan.kr(0),\amp.kr(0.5));             // is this the best place for spatialization ?!?!?
					sig = HPFSides.ar(sig,80);

					Out.ar(\out.kr(0),sig)
				}).add;

				if(print,{ "added: %".format(synthKey).postln });
			})
		})
	});

	// envs before processing
	dcySrc.keysValuesDo({ |srcKey, srcFunc|
		post.keysValuesDo({ |postKey, postFunc|
			env.do({ |envFunc|
				var synthKey = srcKey ++ postKey;

				SynthDef(synthKey.asSymbol,{
					var env = SynthDef.wrap(envFunc,prependArgs: [ 0 ]);
					var sig = SynthDef.wrap(srcFunc, prependArgs: [ In.ar( \inBus.kr(0) ), env ]);
					var lfo = SynthDef.wrap( mod );
					sig = SynthDef.wrap(postFunc, prependArgs: [ sig, lfo ]);
					sig = sig.tanh;
					sig = LeakDC.ar(sig);

					SynthDef.wrap(silence, prependArgs: [ sig ]);
					sig = Pan2.ar(sig,\pan.kr(0),\amp.kr(0.5));             // is this the best place for spatialization ?!?!?
					sig = HPFSides.ar(sig,80);

					Out.ar(\out.kr(0),sig)
				}).add;

				if(print,{ "added: %".format(synthKey).postln });
			})
		})
	});
}