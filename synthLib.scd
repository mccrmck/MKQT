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
		var env = Env([0,1,1,0],[atk,\length.kr(1) - (atk + rls),rls],\curve.kr(-4)).kr(2);    // this should be used strategically - can be sustained or perc!
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


SynthDef(\mkqtIn,{
	var sig = SoundIn.ar(\inBus.kr(0));
	sig = Mix(sig);
	sig = Compander.ar(sig,sig,\compThresh.kr(0.5),1,\compRatio.kr(2).reciprocal,\compAtk.kr(0.01),\compRls.kr(0.1),\muGain.kr(2));
	Out.ar(\sendBus.kr,sig);
	sig = Pan2.ar(sig,\pan.kr(0),\amp.kr(0));
	Out.ar(\out.kr(0),sig)
}).add;

SynthDef(\mkqtAnalysis,{                      // must test this -> can I pass the input bus to the sent OSC msg? How else can I get it to register a trig for the right channel?
	var sig = In.ar(\inBus.kr());
	// var onsets = FluidOnsetSlice.ar(sig); // go through these, do some testing if possible???
	// var specChange = FluidNoveltySlice.ar(sig); // must check this - can I make the buf sizes big enough that it will only trigger at big(ish) changes
	var time = Sweep.ar(\reset.tr(1));

	SendReply.ar(onsets + specChange,'/address/goes/here',[onsets,specChange,time])

}).add


SynthDef(\masterOut,{
	var sig = In.ar(\inBus.kr(0),2);
	sig = sig.tanh;
	sig = HPFSides.ar(sig,\sidesHPFreq.kr(80));
	sig = LeakDC.ar(sig);
	sig = Compander.ar(sig,sig,\compThresh.kr(0.5),1,\compRatio.kr(2).reciprocal,\compAtk.kr(0.01),\compRls.kr(0.1),\muGain.kr(2));
	// what else? Limiter. or?
	Out.ar(\out.kr(0),sig)
}).add;


// Env([0,1,1,0],[atk,\length.kr(1) - (atk + rls),rls],\curve.kr(-4)).kr(2);
Dictionary[

	'bpFreeze' -> { |in, env|
		var sig = 4.collect({ |i| BPF.ar(in,\freq.kr(200) * [3,5,5.5,7].at(i),0.1,2) });
		sig = FFT({ LocalBuf(8192) } ! 4,sig);
		sig = PV_Freeze(sig,1);
		sig = IFFT(sig);
		sig = sig * env;
		sig = Splay.ar(sig,1,\amp.kr(1));
	},

	'verb' -> { |in, env|
		var	sig = HPF.ar(in,120);
		sig = AllpassC.ar(sig,0.2,0.2);
		sig = FreeVerb.ar(sig ,1,0.95,0.95);
		sig = BLowShelf.ar(sig,250,1,-12);
		sig = BHiShelf.ar(sig,1200,1,-12);
		sig = sig * env;
		sig = Pan2.ar(sig,\pan.kr(0),\amp.kr(1) * -6.dbamp)
	}

	'clipDelayDetune' -> { |in,ev|
		var gain = \gain.kr(30);
		var sig = HPF.ar(in,750) * Env.sine().kr;
		sig = BHiShelf.ar(sig,2500,1,6);
		sig = CombC.ar(sig,0.5,\delay.kr(0.3) * XLine.ar(1,1.11,10),\decay.kr(3.0));
		sig = (sig * gain).clip2 * 0.1;
		sig = sig * env;
		sig = Pan2.ar(sig,\pan.kr(0),\amp.kr(1))
	}
]


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













