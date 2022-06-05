

(
//Make a dummy data set
d = FluidDataSet(s);
~data = Dictionary.with(*Array.iota(20).reshape(4,5).collect{|a,i| ("rock_"++i)->a});
~dsdata = Dictionary.newFrom([\cols,4,\data,~data]);
d.load(~dsdata);
d.print;
)


~ds.write("/Users/mikemccormick/Library/Application Support/SuperCollider/Extensions/MKQT/dataSets/rock.json")
~data.size

.fromBuffer
c = FluidLabelSet(s)
d.getIds(c)
c.print

y.print
d.getIds(y).print
y.size


~mlpclassifier = FluidMLPClassifier(s,[5],1,learnRate:0.05,batchSize:5,validation:0.1);


// automate this: while loss > 0.01,{fit} or whatever
// change these throughout the process if necessary!
~mlpclassifier.learnRate = 0.03;
~mlpclassifier.momentum = 0;
~mlpclassifier.validation= 0.2;

~mlpclassifier.fit(d,c,{
	arg loss;
	loss.postln;
});

{
	var trig = Impulse.kr(5);
	var point = WhiteNoise.kr(1.dup);
	var inputPoint = LocalBuf(2);
	var outputPoint = LocalBuf(1);

	Poll.kr(trig, point, [\pointX,\pointY]);
	FluidKrToBuf.kr(point,inputPoint);
	~classifier.kr(trig,inputPoint,outputPoint);
	Poll.kr(trig,BufRd.kr(1,outputPoint,0,interpolation:0),\cluster)
	Silent.ar;
}

(
SynthDef(\mkqtPredict,{
	var trig = Impulse.kr(5); // can make into an Arg later on...?
	var statsBuf = LocalBuf(14);
	var outBuf = LocalBuf(1);
	var sig = SoundIn.ar(\inBus.kr(0));
	sig = FluidSpectralShape.kr(sig, minFreq: 20);
	sig = FluidStats.kr(sig,20); // should experiment with history window size!!


	FluidKrToBuf.kr(sig,statsBuf); // must check what kind of buffer this outputs... 14 frames/1 chan, or 1 frame/14 chans???
	statsBuf.numChannels.postln;
	// ~mlpclassifier.kr(trig, statsBuf, outBuf); // has to be the same instance that was trained!!
	sig = FluidBufToKr.kr(outBuf);

	// SendReply.kr(trig,'/needs/address',[ sig ])

}).add
)


SynthDef(\test,{
	var sig = LFSaw.kr();
	FluidStats.kr(sig,20);
}).send

Synth(\test)

.send

{FluidStats.kr(SinOsc.ar(),20)}.play



Ndef(\test,{
	var sig = LFSaw.ar();
	// sig.postln;
	FluidStats.kr(sig,20);
	sig * 0.001
}).play
