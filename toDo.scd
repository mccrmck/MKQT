
// naming convention
/*
(
i = 0 ; //index
c = "class";
d = Date.getDate.format("%M%H%d%m%y");
d = "%_".format(c) ++ d ++ i;
d.postln;
//
d.split($_)[0]

)
*/

/*

GUI
-make save buttons visible only when training is done?


TRAIN         -- should this be changed to DATA instead of train?
-how do we associate synth behaviour with classifiers? Is that done manually during/after training? Is it absolute?

-automate fitting with a routine...can inform band that if they don't like how it's working they can 1. add more data 2. refit to get different results 3. Hire a new programmer??
-can/should I automate Fitting?

PLAY
-should have feedback button (map to spacebar?) that adds a point to dataset
-timed sections, change instruments/intensity/aesthetics

-MLPClassifier runs at .kr and tries to classify behaviour based on available choices... must have some filtering (sample and hold?) to ensure longer phrases and not wild switching
-switch statement a la EIDOLON makes decisions about what kind of processing happens.

*/


Ndef(\masterOut,{
	var sig = In.ar(\inBus.kr(0),2);
	sig = sig.tanh;
	sig = HPFSides.ar(sig,\sidesHPFreq.kr(80));
	sig = LeakDC.ar(sig);
	sig = Compander.ar(sig,sig,\compThresh.kr(0.5),1,\compRatio.kr(2).reciprocal,\compAtk.kr(0.01),\compRls.kr(0.1),\muGain.kr(2));
	// what else? Limiter. or?
	Out.ar(\out.kr(0),sig)
}).play;




// make synthLib classvar with keys from the synthLib - this can aid the process when adding new genre dataSets
// make OSCdef that assigns the genreState variable
// make OSCdefs that choose synths from lib based on genreState
// what happens at time boundaries? How does the "sound aesthetic" change?
// does the synthLib have args in each event as well? Or are they just arg indexes? Or how do I handle this smartly/algorithmically?

// synthLib needs a default key for when new datasets return a genre that doens't have synths yet!
// when data/Label set is created, it builds a synth Library also - anything that is nil gets filled with Default synths!


~buf = Buffer.read(s,"/Users/mikemccormick/Dropbox/Mac/Desktop/Projects/ternOP/Let them eat the Rich/WAV 010222/Master 010222- 48k24b -04- Let he who is without sin live in a glass house.wav")
Ndef(\mkqtPredict).clear

(
Ndef(\mkqtPredict,{
	var statsBuf = LocalBuf(14);
	var outBuf = LocalBuf(1);
	// var sig = In.ar(\inBus.kr(0),2);
	var in = PlayBuf.ar(2,~buf,BufRateScale.kr(~buf),startPos: BufFrames.kr(~buf) * 0.5, loop: 1);
	var sig = in.sum * -3.dbamp;
	var trigGate = (FluidLoudness.kr(sig)[0] > \trigGateThresh.kr(-18) );
	var trig = Impulse.kr(\trigRate.kr(10) * trigGate);
	sig = FluidSpectralShape.kr(sig, minFreq: 20);
	sig = FluidStats.kr(sig,20).flat; // should experiment with history window size!!

	FluidKrToBuf.kr(sig,statsBuf);
	MKQT.mlp.kr(trig, statsBuf, outBuf); // has to be the same instance that was trained!!

	sig = FluidBufToKr.kr(outBuf);

	SendReply.kr(trig,'/needs/address',[ sig ]);

	in
}).play
)

// fibonacci barlines
Ndef(\clock,{
	var trig = Impulse.kr(1);
	var time = Sweep.ar(\reset.tr(1));
	SendReply.kr(trig,'/fibonacci',[ time ]);
});

OSCdef(\clockListener,{ |msg, time, addr, recvPort|
	var timeSinceStart = msg[3].round(1);
	var fibArray = Array.fib(9,1,1) * 60;
	timeSinceStart.postln;

	fibArray.do({ |fibNum|
		if(timeSinceStart == fibNum,{
			MKQT.synthLib.keysValuesChange({ |key,synthKeys|
				synthKeys.rotate(-1)
			})
		})
	});

},'/fibonacci')


OSCdef(\test,{ |msg, time, addr, recvPort|
	var classIndex = msg[3];                               // add a median filter here ?!?!?!
	MKQT.classifierIndex = classIndex;

	if(MKQT.verbose,{ MKQT.classifiers[ classIndex ].postln })
},'/needs/address')

MKQT.verbose = true




classVar = ['Jan','Flo','Karl'].collect({ |name, index|
	var rcvAddr = ("/mkqtRespond" ++ name).asSymbol;

	OSCFunc({ |msg, time, addr, recvPort|
		var onsetTrig =  msg[3];
		var specTrig =  msg[4];

		[onsetTrig,specTrig].postln;

		if( MKQT.prob.coin,{
			var synthKey = MKQT.classifiers[ MKQT.classifierIndex ];


			// something genreState ? 'default' ... does this go here?


			// Synth instance....args??
			// this can also be 4.do({ Synth instance w/ randArgs }), especially if they are grain-ish processes

			// if(verbose,{ synthKey.postln })

		});

	},rcvAddr)
})

*startNdefs {

	['Jan','Flo','Karl'].do({ |name, index|
		var key = ("mkqtIn" ++ name).asSymbol;
		var sendAddr = ("/mkqtRespond" ++ name).asSymbol;

		Ndef(key,{                                               // compression may not be necessary pga. digital instruments...
			var sig = SoundIn.ar(\inBus.kr(0));
			sig = Mix(sig);
			sig = Compander.ar(sig,sig,\compThresh.kr(0.5),1,\compRatio.kr(2).reciprocal,\compAtk.kr(0.01),\compRls.kr(0.1),\muGain.kr(2));
			sig = Pan2.ar(sig,\pan.kr(0),\amp.kr(0));
			Out.ar(\out.kr(0),sig)
		}).play(out: MKQT.mainOut);

		Ndef(key).filter(1,{ |in|
			var sig = in.sum * -3.dbamp;
			var onsets = FluidOnsetSlice.ar(sig); // go through these, do some testing if possible???
			var specChange = FluidNoveltySlice.ar(sig,0,11,0.33); // must check this - can I make the buf sizes big enough that it will only trigger at big(ish) changes

			SendReply.ar(onsets + specChange,sendAddr,[onsets,specChange]);
			in;
		})
	})
}





