
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


// Fix the default thing!! it's fucking up the classifier indexes!! But is probably a good idea!

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

Platform.userExtensionDir.openOS

