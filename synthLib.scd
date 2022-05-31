(
// what's the best way to do this? Normalize args and store them in another part of the event?
var ugenGraphs = Dictionary[

	'bpFreeze' -> { |in, env|
		var sig = 4.collect({ |i| BPF.ar(in,\freq.kr(200) * [3,5,5.5,7].at(i),0.1,2) });
		sig = FFT({ LocalBuf(8192) } ! 4,sig);
		sig = PV_Freeze(sig,1);
		sig = IFFT(sig);
		sig = sig * env;
		// Out.ar(\send.kr,sig.sum * \sendAmp.kr(0));  // do we want this here?
		sig = Splay.ar(sig,1,\amp.kr(1));
	},
];

var makeDef = { | name, ugenGraph|
	SynthDef(name.asSymbol,{
		var atk = \atk.kr(0.01), rls = \rls.kr(1);
		var env = Env([0,1,1,0],[atk,\length.kr(1) - (atk + rls),rls],\curve.kr(-4)).kr(2);
		var sig = SynthDef.wrap(ugenGraph, prependArgs: [ In.ar(\inBus.kr(0)), env ]);

		sig = sig.tanh;
		Out.ar(\out.kr(0),sig)
	}).add
};

ugenGraphs.keysValuesDo({ |synthName, synthFunc|

	makeDef.(synthName, synthFunc);

	"% synths added".format(synthName).postln;
});
)


SynthDescLib.global.browse

~buf = Buffer.read(s,FluidFilesPath("Olencki-TenTromboneLongTones-M.wav"))


// should I use synth.variants to explore different envelop shapes? Or do it language side?

SynthDef(\in,{
	var sig = SoundIn.ar(\inBus.kr(0));
	sig = Compander.ar(sig,sig,\compThres.kr(0.5),1,\compRatio.kr(2).reciprocal,\compAtk.kr(0.01),\compRls.kr(0.1),\muGain.kr(2));
	Out.ar(\out.kr(0),sig)
}).add;


SynthDef(\masterOut,{
	var sig = In.ar(\inBus.kr(0),2);
	sig = HPFSides.ar(sig,\sidesHPFreq.kr(80));
	sig = LeakDC.ar(sig);
	// what else? Compressor?
	Out.ar(\out.kr(0),sig)
}).add;

d = Date.getDate.format("%M%H%d%m%y")
e = "%_".format("jazz") ++ d;
e.split($_)[0]



