
MKQTGUI.startGUI
MKQTGUI.playGUI

MKQT.mlp.hidden = [12,8,5]
MKQT.mlp.learnRate = 0.001
MKQT.mlp.momentum = 0.02
MKQT.mlp.batchSize = 15

MKQT.synthLib.dopostln
MKQT(0,1,[4,5])

MKQT.classifiers
MKQT.mainDataSet.print
MKQT.mainLabelSet.print

s.plotTree

/*
TRAIN    -- should this be changed to DATA instead of train?

-can/should I automate Fitting?

PLAY
-should have feedback button (map to spacebar?) that adds a point to dataset
-timed sections, change instruments/intensity/aesthetics

- do I need median filtering (or sample and hold?) to ensure longer phrases and not wild switching
-switch statement a la EIDOLON makes decisions about what kind of processing happens.
-synthLib needs a default key for when new datasets return a genre that doens't have synths yet!

*/

// must check signal path - if fades for each player are down, does the analysis still get signal? I think not....


// Fix the default thing!! it's fucking up the classifier indexes!!

s.sampleRate

OSCFunc.trace(false)
NetAddr.localAddr

s.sendMsg("/n_free",1388)

(
3.do({ |i|
	OSCdef("test%".format(i).asSymbol,{|msg, time, addr, recvPort|
		var val = msg[1];

		MKQT.prob[i] = msg[1]/10;
		/*[i,val/10].postln;*/

	},"/testMKQT/%".format(i).asSymbol)
})
)

