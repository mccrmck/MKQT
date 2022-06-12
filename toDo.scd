
/*


TRAIN         -- should this be changed to DATA instead of train?

-automate fitting with a routine...can inform band that if they don't like how it's working they can 1. add more data 2. refit to get different results 3. Hire a new programmer??
-can/should I automate Fitting?

PLAY
-should have feedback button (map to spacebar?) that adds a point to dataset
-timed sections, change instruments/intensity/aesthetics

-MLPClassifier runs at .kr and tries to classify behaviour based on available choices... must have some filtering (sample and hold?) to ensure longer phrases and not wild switching
-switch statement a la EIDOLON makes decisions about what kind of processing happens.
-synthLib needs a default key for when new datasets return a genre that doens't have synths yet!

*/



~buf = Buffer.read(s,"/Users/mikemccormick/Dropbox/Mac/Desktop/Projects/ternOP/Let them eat the Rich/WAV 010222/Master 010222- 48k24b -04- Let he who is without sin live in a glass house.wav")
Ndef(\mkqtPredict).clear

(
Ndef(\mkqtPredict,{
	var statsBuf = LocalBuf(14);
	var outBuf = LocalBuf(1);
	var sig = In.ar(MKQT.mainOut,2);
	var sig = in.sum * -3.dbamp;
	var trigGate = (FluidLoudness.kr(sig)[0] > \trigGateThresh.kr(-18) );
	var trig = Impulse.kr(\trigRate.kr(10) * trigGate);
	sig = FluidSpectralShape.kr(sig, minFreq: 20);
	sig = FluidStats.kr(sig,20).flat; // should experiment with history window size!!

	FluidKrToBuf.kr(sig,statsBuf);
	MKQT.mlp.kr(trig, statsBuf, outBuf); // has to be the same instance that was trained!!

	sig = FluidBufToKr.kr(outBuf);

	SendReply.kr(trig,'/needs/address',[ sig ]);

	// in
})
)

OSCdef(\test,{ |msg, time, addr, recvPort|
	var classIndex = msg[3];                               // add a median filter here ?!?!?!
	MKQT.classifierIndex = classIndex;

	if(MKQT.verbose,{ MKQT.classifiers[ classIndex ].postln })
},'/needs/address')
