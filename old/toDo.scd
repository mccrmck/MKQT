
MKQTGUI.startGUI
MKQTGUI.playGUI

MKQT.mlp.hidden = [12,8,5]
MKQT.mlp.learnRate = 0.001
MKQT.mlp.momentum = 0.02
MKQT.mlp.batchSize = 15

MKQT.synthLib.dopostln
MKQT(0,1,[4,5])

MKQT.classifiers
MKQT.classifierIndex
MKQT.mainDataSet.print
MKQT.mainLabelSet.print
MKQT.mlp


s.plotTree

s.options.numInputBusChannels = 64
s.options.numOutputBusChannels = 64

s.sampleRate

OSCFunc.trace(false)
NetAddr.localAddr


//single probs
s.sendMsg("/n_free",6338)

MKQT.prob = [0.05,0.05,0.05]
MKQT.prob = [0.08,0.08,0.08]
MKQT.prob = [0.1,0.1,0.1]



(
3.do({ |i|
	OSCdef("test%".format(i).asSymbol,{|msg, time, addr, recvPort|
		var val = msg[1];

		MKQT.prob[i] = msg[1]/10;
		/*[i,val/10].postln;*/

	},"/testMKQT/%".format(i).asSymbol)
})
)

MKQT.prob

Quarks.gui
