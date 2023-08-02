MKQTSynth {

	classvar <preFunc, <postFunc;

	*initClass {
		StartUp.add({
			this.makeSynthDefs;
		});

		preFunc = [

			(
				key: 'thru',
				func: { |sig| sig.tanh }
			),(
				key: 'freeze',
				func: { |sig|
					sig = FFT({ LocalBuf(2048) }!2,sig);
					sig = PV_Freeze(sig,1);
					IFFT(sig);
				}
			),(
				key: 'sineLoop',
				func: { |sig|
					sig = sig * Env.sine(0.8).ar;      // map to an arg??
					sig = LocalIn.ar(2) + sig;
					sig = DelayC.ar(sig,0.75,\delay.kr(0.18).linlin(0,1,0.08,0.75));            // map this better and make it an argument
					LocalOut.ar(sig) * 0.98;
					sig
				}
			),(
				key: 'percLoop',
				func: { |sig|
					sig = sig * Env.perc(0.01,0.6).ar;      // map to an arg??
					sig = LocalIn.ar(2) + sig;
					sig = DelayC.ar(sig,0.75,\delay.kr(0.18).linlin(0,1,0.08,0.75));            // map this better and make it an argument
					LocalOut.ar(sig) * 0.98;
					sig
				}
			),(
				key: 'delay',
				func: { |sig|
					CombC.ar(sig,0.5,\delay.kr(0.25).linexp(0,1,0.1,0.5),5)
				}
			)
		];

		postFunc = [                                        // can make these lower case keys and later use string[0].toUpper
			(
				key: 'Am',
				func:  {|sig, mod|
					sig * LFPulse.ar(\freq.kr(0.25).linexp(0,1,4,18) * mod).clip2
				},
			),(
				key: 'Bits',
				func: { |sig, mod|
					Decimator.ar(sig,12000,\freq.kr(1).linlin(0,1,4,12) * mod).clip2
				},
			),(
				key: 'Bpf',
				func: { |sig, mod|
					BPF.ar(sig,\fFreq.kr(0.5).linexp(0,1,500,1200) * mod,2.sqrt,2).clip2
				},
			),(
				key: 'Comb',
				func: { |sig, mod|
					CombC.ar(sig,0.2,\freq.kr(0).linexp(0,1,40,250).reciprocal * mod,-2);
				},
			),(
				key:'Degrade',
				func: { |sig, mod|
					Latch.ar(sig,Impulse.ar(\freq.kr(0.5).linexp(0,1,1200,10000) * mod));
				}
			),(
				key: 'FreqShift',
				func: { |sig, mod|
					FreqShift.ar(sig,\freq.kr(0.6).linlin(0,1,-250,250) * mod)
				},
			),(
				key: 'PitchShift',
				func: { |sig, mod|
					PitchShift.ar(sig,0.1,\freq.kr(0).linexp(0,1,0.5,2) * mod)
				},
			),(
				key: 'Ring',
				func: { |sig, mod|
					sig * SinOsc.ar(\freq.kr(0.5).linexp(0,1,40,600) * mod);
				},
			),(
				key: 'Rlpf',
				func: { |sig, mod|
					RLPF.ar(sig,\freq.kr(0.5).linexp(0,1,600,3000) * mod,0.2,2).clip2
				},
			)
		];
	}

	*new { |inBus, outBus, target ...args|                                                     // 10 normalized values from an MLP
		^super.new.init(inBus, outBus, target, args)
	}

	init { |in, out, targ, args|
		var busArgs   = [\inBus,in,\outBus,out];
		var preIndex  = args[0].linlin(0,1,0,preFunc.size).floor.clip(0,preFunc.size-1);
		var postIndex = args[1].linlin(0,1,0,postFunc.size).floor.clip(0,postFunc.size-1);
		var synthKey  = preFunc[preIndex]['key'] ++ postFunc[postIndex]['key'];

		var synthArgs = [[\delay, \freq, \modFreq, \modIndex, \atk, \rls, \curve, \pan], args[2..]].flop.flat;

		synthArgs = busArgs ++ synthArgs;

		^Synth(synthKey,synthArgs,targ,'addToTail')
	}

	*makeSynthDefs {
		var mod = {
			var modFreq = \modFreq.kr(0.5).linexp(0,1,0.5,12);
			var synthArray = [
				1,
				LFNoise0.ar(modFreq).range(0.9,1),
				LFNoise0.ar(modFreq / 2).range(0.45,0.5),
				// LFNoise1.ar(modFreq).range(0.9,1),
				Line.ar(0.5,1,10),
				1,
				// LFSaw.ar(modFreq).range(0.9,1)
			];
			Select.kr(\modIndex.kr(0).linlin(0,1,0,synthArray.size).floor,synthArray)
		};

		var env = {
			var envelope = Env(
				[0,1,0],
				[ \atk.kr(0).linlin(0,1,0.01,8), \rls.kr(0.25).linlin(0,1,0.01,8) ],
				\curve.kr(0).linlin(0,1,-10,10),
			);
			EnvGen.ar(envelope, doneAction: 2)
		};

		preFunc.do({ |preFuncEvent,index|
			var preKey  = preFuncEvent['key'];
			var preFunc = preFuncEvent['func'];

			postFunc.do({ |postFuncEvent,index|
				var postKey  = postFuncEvent['key'];
				var postFunc = postFuncEvent['func'];
				var synthKey = preKey ++ postKey;

				SynthDef(synthKey.asSymbol,{
					var sig = In.ar(\inBus.kr(0),2);
					var lfo = mod.value;
					sig = preFunc.value(sig);
					sig = postFunc.value(sig, lfo );
					sig = Squish.ar(sig,sig,-18,0.03,0.1,4,0,0).tanh;
					sig = LeakDC.ar(sig);

					sig = sig * env.value;
					sig = Balance2.ar(sig[0],sig[1],\pan.kr(0).linlin(0,1,-0.5,0.5));
					sig = HPFSides.ar(sig,80);

					Out.ar(\outBus.kr(0),sig)
				}).add;
			})
		});
	}
}

