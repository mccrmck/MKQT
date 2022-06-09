

~buf = Buffer.read(s,FluidFilesPath("Olencki-TenTromboneLongTones-M.wav"))
~buf = Buffer.read(s,FluidFilesPath("Nicol-LoopE-M.wav"))

(
Ndef(\test,{
	var bufnum = ~buf;
	var frames = BufFrames.kr(bufnum);
	var in = PlayBuf.ar(1,bufnum,BufRateScale.kr(bufnum),startPos: frames * 0.2,loop: 1);

	var sig = in * Env.sine().kr;

	sig = LocalIn.ar(1) + sig;
	sig = DelayC.ar(sig,0.2,0.16);

	LocalOut.ar(sig) * 0.8;

	sig = (sig * XLine.kr(1,30,10));


	// sig = sig * env;
	sig = Pan2.ar(sig,\pan.kr(0),\amp.kr(1))

}).play
)



// single pShift delay
// long pshift delay


// at some point, can I maybe reorganize these synths?
// Some will have a CombC + processing
// some will have a DelayC fbLoop + processing
// some will have a PV_Freeze + processing
// maybe make a SynthDef.wrap function to get a bunch of different behaviours from the same starting blocks?




(
var src = Dictionary[

	'freeze' -> { |sig|
		sig = FFT(LocalBuf(4096),sig);
		sig = PV_Freeze(sig,1);
		sig = IFFT(sig);
	},

	'oneTap' -> { |sig|
		DelayC.ar(sig,0.5,\dly.kr(0.2));
	},

	'fbDel' -> { |sig|
		CombC.ar(sig,0.5,\dly.kr(0.2),\dcy.kr(1));
	},

	'modDel' -> { |sig|
		CombC.ar(sig,0.5,\dly.kr(0.2) * XLine.kr(1,Select.kr(\detuneIndex.kr(1),[0.9,1.1]),10),\dcy.kr(1));
	},

	'loop' -> { |sig|
		sig = sig * Env.sine().kr;
		sig = LocalIn.ar(1) + sig;
		sig = DelayC.ar(sig,0.75,\dly.kr(0.2));
		LocalOut.ar(sig) * 0.9;
		sig
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

	'Rlpf' -> { |sig|
		RLPF.ar(sig,\fFreq.kr(400),\rq.kr(0.5),4).clip2
	},

	'BitCrush' -> { |sig|
		Decimator.ar(sig,SampleRate.ir * \sRate.kr(0.5),Bit )
	}

];

var env = Dictionary[
	'Perc' -> { Env.perc(\atk.kr(0.01),\rls.kr(1),1,\curve.kr(-4)).kr(2) },
	'Sus' -> {
		var atk = \atk.kr(0.01), rls = \rls.kr(1);
		Env([0,1,1,0],[atk,\length.kr(4) - (atk + rls),rls],\curve.kr(-4)).kr(2)
	},
];

var makeDef = { |name, srcFunc, postFunc, envFunc|
	SynthDef(name.asSymbol,{

		var env = SynthDef.wrap(envFunc);
		var sig = SynthDef.wrap(srcFunc, prependArgs: [ In.ar( \inBus.kr(0) )]);
		sig = SynthDef.wrap(postFunc, prependArgs: [ sig ]);

		sig = sig * env;
		sig = sig.tanh;

		Out.ar(\out.kr(0),sig)
	}).add
};

src.keysValuesDo({ |srcKey, srcFunc|
	post.keysValuesDo({ |postKey, postFunc|
		env.keysValuesDo({ |envKey, envFunc|
			var synthKey = srcKey ++ postKey ++ envKey;

			makeDef.(synthKey.asSymbol,srcFunc,postFunc,envFunc);

			"added: %".format(synthKey).postln;

		})
	})
});
)

// should the output amplitude be controlled somehow? Compander? Amplitude compensation?
// normalize all args!!

a = SynthDescLib.global[\modDelClipSus].controlNames.reject({ |cName|
	cName == 'inBus' ||
	{ cName == 'out'} ||
	{ cName == 'out'} ||
	{ cName == 'out'} ||

})
a
