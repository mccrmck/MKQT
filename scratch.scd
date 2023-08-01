


n = NetAddr("192.168.0.132",9000);
n = NetAddr("172.20.10.2",9000);
n = NetAddr("169.254.74.246",9000);

(
r = Routine({
	var delta = 1/30;
	loop{
		var array = Array.rand(19,0,0.2);
		delta.wait;
		// array.postln;
		n.sendMsg("/mkqt",*array)
	}
}).play
)

r.stop

ServerOptions.inDevice

OSCFunc.trace


MKQT2()

s.plotTree
m = MKQTPlay(0,1,[4,5],[0,1])

m.startPerformance(0);
m.startOSCVisuals("172.20.10.2",9000,1/30);
m.stopOSCVisuals

m.stopPerformance
m.mixer['karl'].amp = 0
m.mixer.amp = -22.dbamp

FluidNoveltySlice
m.mixer['jan'].inserts['janTrigger'].set(\onsetThresh,0.1,\noveltyThresh,0.2)
m.mixer['flo'].inserts['floTrigger'].set(\onsetThresh,0.2)
m.mixer['karl'].inserts['karlTrigger'].set(\onsetThresh,0.5,\noveltyThresh,0.2)

m.strips.keysDo({|i| i.postln})

(
var prevTime = 0;
f = File("~/oscMsg3.txt".standardizePath,"w");

OSCdef(\write,{ |msg,time|
	var delta;
	if(prevTime != 0,{
		delta = time - prevTime;
	},{
		delta = 0;
	});

	prevTime = time;
	// delta.postln;

	f.write("delta,vals: [%,%],\n".format(delta,msg[1..]));
	// msg.postln;

},'/write')
)

f = File("~/oscMsg.txt".standardizePath,"w");
f.write("test")
f.close

// seems like maybe visuals are being fed trio, not naomi?
// check the sendStrip -> .addInSynth change!! Update accordingly...
// needs two analysis sends! one for band that updates analysisBuf, one for updating visuals
// performance duration?
// reverb sendStrip
// fix GUI -> onset calibration, novelty?
// what do I do about the coin probability?
// Karl & Jan's mlps needs more training (maybe more data points also?)
// must filter out double onset triggers, cool down period for triggers?
// change after a certain amount of time...new MLP?!