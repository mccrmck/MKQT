


n = NetAddr("192.168.0.132",9000);
n = NetAddr("172.20.10.2",9000);

(
r = Routine({
	var delta = 1/30;
	loop{
		var array = Array.rand(19,0,0.5);
		delta.wait;
		n.sendMsg("/mkqt",*array)
	}
}).play
)

r.stop


MKQTv2()

s.plotTree
m = MKQTPlay(0,1,[4,5],[0,1])
MKQTv2.playOSCdefs(m)

m.startPerformance;
m.stopPerformance;
m.startOSCVisuals("192.168.0.132",9000,1/30);
m.stopOSCVisuals

MKQTGUI.startGUI

// "reverse delay" effect -> more of that!
// change after a certain amount of time...new MLP?!
// tabbed interface for trainingPanel -> add points to new dataset, append to old dataset, train new MLP, retrain MLP on existing data
// can back button reset GUI?
// make o-s-c send a free/close message to SC!

~ds0 = FluidDataSet(s).read("/Users/mikemccormick/Library/Application Support/SuperCollider/Extensions/MKQT/newDS/230731_223536flo_analyDS.json")
~ds1 = FluidDataSet(s).read("/Users/mikemccormick/Library/Application Support/SuperCollider/Extensions/MKQT/newDS/230731_230821jan_synthDS.json")

~ds0.size
~ds1.dump({|d| d["data"]["0"].postln })


~mlp = FluidMLPRegressor(s, [15,12], FluidMLPRegressor.relu, FluidMLPRegressor.relu,learnRate: 0.1);  // fix layer structure...?
~mlp.fit(~ds0,~ds1,{|loss| loss.postln})


~mlp.write("/Users/mikemccormick/Library/Application Support/SuperCollider/Extensions/MKQT/newMLP/%jan.json".format(Date.getDate.stamp))



~mlp = FluidMLPRegressor(s).read("/Users/mikemccormick/Library/Application Support/SuperCollider/Extensions/MKQT/MLPs/flo.json")

1/750


OSCdef('test').clear

(
~src = Buffer.loadCollection(s,Array.rand(19,0,1.0));
~targ = Buffer.loadCollection(s,Array.rand(10,0,1.0));
x = { |delta|

	fork{
		loop{
			~src.setn(0,Array.rand(10,0,1.0));

			~mlp.predictPoint(~src,~targ,{
				~targ.getn(0,10,action:{ |array|
					array.postln
				})
			});
			delta.wait;
		}
	}
}
)

x.(0.05)
x.stop



b = Buffer.loadCollection(s,Array.fill(19, {1.0.rand} ))
bench{ 100.do({b.getn(0,10,{|a|})}) }

