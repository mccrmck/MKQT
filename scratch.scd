




~coefs = 13;

~pointCount = 0;

~mfccBuf = Buffer.alloc(s,~coefs);
~dataSet = FluidDataSet(s);
~labelSet = FluidLabelSet(s);


y = FluidLabelSet(s);

g = d.getIds

(
//Make a dummy data set
d = FluidDataSet(s);
~data = Dictionary.with(*Array.iota(20).reshape(4,5).collect{|a,i| ("row"++i)->a});
~dsdata = Dictionary.newFrom([\cols,4,\data,~data]);
d.load(~dsdata);
d.print;
)

y.print
d.getIds(y).print
y.size

// this will of course have another inSynth before where it can send to both the trainer and the predicter via \sendBus.kr

SynthDef(\trainMKQTmfcc,{
	var coefs = \coefs.kr;
	var sig = SoundIn.ar(\inBus.kr);
	var mfccs = FluidMFCC.kr(sig,coefs,40,1,maxNumCoeffs:coefs); // remove 0th coef
	FluidKrToBuf.kr(mfccs,\bufnum.kr);
	SendReply.kr(Impulse.kr(30),'/addPoints',mfccs);
}).add



OSCdef(\addPoints,{ |msg, time, addr, recvPort|
	var label = "chill";                                 // must be able to pass this!!
	var id = "%_%".format(label,~pointCount);         // id could probably just be an integer
	~dataSet.addPoint(id,~mfccBuf);
	~labelSet.addLabel(id,label);
	~pointCount = ~pointCount + 1;
},'/addPoints')

// IMPORTANT - labels get an index in the labelSet Dictionary, so I must be able to store these indexes somewhere for future recall!!
// make use of Archive.global for saving these values!!


~mlpclassifier = FluidMLPClassifier(s,[5],1,learnRate:0.05,batchSize:5,validation:0.1);


//automate this: while loss > 0.01,{fit} or whatever
// change these throughout the process if necessary!
~mlpclassifier.learnRate = 0.01;
~mlpclassifier.momentum = 0;
~mlpclassifier.validation= 0.2;

~mlpclassifier.fit(~dataSet,~labelSet,{
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

FluidBufToKr

SynthDef(\predictMKQT,{
	var coefs = \coefs.kr;
	var trig = Impulse.kr(5); // can make into an Arg later on..
	var inBuf = LocalBuf(coefs);
	var outBuf = LocalBuf(1);
	var sig = SoundIn.ar(\inBus.kr);
	sig = FluidMFCC.kr(sig,coefs,40,1,maxNumCoeffs:coefs); // remove 0th coef
	FluidKrToBuf.kr(sig,inBuf);
	~mlpclassifier.kr(trig, inBuf, outBuf); // has to be the same instance as earlier!! Also, I think this will output one point that has to be referenced with the indexes of the LabelSet ids!!


}).add







.sync