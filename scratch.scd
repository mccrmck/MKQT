

(
//Make a dummy data set
d = FluidDataSet(s);
~data = Dictionary.with(*Array.iota(20).reshape(4,5).collect{|a,i| ("rock_"++i)->a});
~dsdata = Dictionary.newFrom([\cols,4,\data,~data]);
d.load(~dsdata);
d.print;
)

c = FluidLabelSet(s)
d.getIds(c)
c.print

d.print
d.getIds(y).print
y.size


~mlpclassifier = FluidMLPClassifier(s,[5],1,learnRate:0.05,batchSize:5,validation:0.1);


// automate this: while loss > 0.01,{fit} or whatever
// change these throughout the process if necessary!
~mlpclassifier.learnRate = 0.03;
~mlpclassifier.momentum = 0.4;
~mlpclassifier.validation= 0.2;

~mlpclassifier.fit(d,c,{
	arg loss;
	loss.postln;
});
