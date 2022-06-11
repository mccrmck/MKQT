{ |print|
	var susSrc = Dictionary[

		// 'sine' -> { SinOsc.ar(440) },

		'freeze' -> { |sig|
			var frames = \frames.kr(1024);
			sig = FFT(LocalBuf(frames),sig);
			sig = PV_Freeze(sig,1);
			sig = IFFT(sig);
		},

		'loop' -> { |sig|
			sig = sig * Env.sine().kr;
			sig = LocalIn.ar(1) + sig;
			sig = DelayC.ar(sig,0.75,\dly.kr(0.2));
			LocalOut.ar(sig) * 0.9;
			sig
		},
	];

	var dcySrc = Dictionary[

		'oneTap' -> { |sig, env|
			DelayC.ar(sig * env,0.5,\dly.kr(0.2));
		},

		'fbDel' -> { |sig, env|
			CombC.ar(sig * env,0.5,\dly.kr(0.2),\dcy.kr(1));
		},

		'modDel' -> { |sig, env|
			CombC.ar(sig * env,0.5,\dly.kr(0.2) * XLine.kr(1,Select.kr(\detuneIndex.kr(1),[0.9,1.1]),10),\dcy.kr(1));
		},
	];

	var post = Dictionary[

		'Ring' -> { |sig|
			sig * SinOsc.ar(\freq.kr(300));
		},

		'FreqShift' -> { |sig|
			FreqShift.ar(sig,\freqDev.kr(25))
		},

		'Comb' -> { |sig|
			CombC.ar(sig,0.2,\freq.kr(120).reciprocal,-1);
		},

		'Clip' -> { |sig|
			(sig * 8).clip2 * -12.dbamp
		},

		'Bpf' -> { |sig|
			BPF.ar(sig,\freq.kr(400),\rq.kr(0.4),2).clip2
		},

		'Rlpf' -> { |sig|
			RLPF.ar(sig,\fFreq.kr(400),\rq.kr(0.5),4).clip2
		},

		'BitCrush' -> { |sig|
			Limiter.ar(Decimator.ar(sig,SampleRate.ir * \sRate.kr(0.25),24 * \bits.kr(0.5) ) )
		},
	];

	var env = { |dAction|
		var envArray = Env([0,1,0],[\atk.kr(0.01), \rls.kr(1)]).asArray;
		envArray[6,10..] = \shape.kr(4);
		envArray[7,11..] = \curve.kr(4);
		EnvGen.kr(envArray, doneAction: dAction)
	};

	var silence = { |sig| DetectSilence.ar(sig,-60.dbamp,0.2,2) };

	// envs after processing
	susSrc.keysValuesDo({ |srcKey, srcFunc|
		post.keysValuesDo({ |postKey, postFunc|
			env.do({ |envFunc|
				var synthKey = srcKey ++ postKey;

				SynthDef(synthKey.asSymbol,{
					var sig = SynthDef.wrap(srcFunc, prependArgs: [ In.ar( \inBus.kr(0) )]);
					sig = SynthDef.wrap(postFunc, prependArgs: [ sig ]);
					sig = sig.tanh;

					sig = sig * SynthDef.wrap(envFunc,prependArgs: [ 2 ]);
					sig = Pan2.ar(sig,\pan.kr(0),\amp.kr(0.5));             // is this the best place for spatialization ?!?!?

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
					sig = SynthDef.wrap(postFunc, prependArgs: [ sig ]);
					sig = sig.tanh;

					SynthDef.wrap(silence, prependArgs: [ sig ]);
					sig = Pan2.ar(sig,\pan.kr(0),\amp.kr(0.5));             // is this the best place for spatialization ?!?!?

					Out.ar(\out.kr(0),sig)
				}).add;

				if(print,{ "added: %".format(synthKey).postln });
			})
		})
	});
}