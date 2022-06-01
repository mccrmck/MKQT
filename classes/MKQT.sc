MKQT {
	classvar <>classifiers;
	classvar <>mainDataSet, <>mainLabelSet;

	*initClass {

		classifiers = List(); // all classifiers get added to this dictionary and saved?

		ServerTree.add({ |server|                         // check if this makes sense...I think Cmd +. will make new instances, is that good/bad???
			mainDataSet = FluidDataSet(server);
			mainLabelSet = FluidLabelSet(server)},
		\default
		)
	}

	// necessary?
	*new {
		^super.new.init
	}

	// necessary?
	init {}                        // maybe I can make Server.default a class arg here? that would save some characters...


	/* ==== data collection ==== */

	*liveData {
		var coefs = 13;

		Routine({
			SynthDef(\trainMKQTLive,{
				var sig = SoundIn.ar(\inBus.kr);
				var mfccs = FluidMFCC.kr(sig,coefs,40,1,maxNumCoeffs:coefs); // remove 0th coef, 13 total (1-13)
				FluidKrToBuf.kr(mfccs,\bufnum.kr);
				SendReply.kr(Impulse.kr(30),'/addPoints',mfccs);
			}).add;

		}).play
		// maxNumCoeffs must be declared when Synth is added - consider making a factory? Or setting a hi max? Or just using a fixed number?

	}

	*bufData {

		// things that have to happen here:
		// store classifier name in classifiers Dict, eventually store dict in Archive...maybe write it immediately?


	}

	/* ==== playing methods ==== */

	*makeDataAndLabelSets { |paths| // array from GUI
		var labelId = 0;
		var names = paths.collect({ |p,i| PathName(p).fileNameWithoutExtension });    		// check for file types? If .json.not, throw an error?
		var dSets = paths.collect({ |p,i| FluidDataSet(Server.default).read(p,{ "dataSet: % loaded".format(names[i]).postln }) });

		names = names.collect({ |name| name.split($_)[0] });


		//sort dSets based on file names? and then handle duplicates by merging them?

		// build the labelSet
		names.do({ |name,index|

			classifiers.add(name.asSymbol);  // MLP is going to spit out label indexees based on the order it sees new labels, must keep track of these!

			dSets[index].size({ |size|
				size.do({ |i|
					this.mainLabelSet.addLabel(labelId,name.asString);  // not sure I need "this"
					labelId = labelId + 1
				});
			})
		});

		// build the dataSet
		this.mainDataSet = this.concatDataSets(dSets) // not sure I need the first "this"
	}

	*concatDataSets { |dataSets|  // must be sorted by label/classifier first...which would be fileName in gui!
		var zeroSet = dataSets[0];

		dataSets[1..].do({ |dSet|
			zeroSet.merge(dSet)
		});
		^zeroSet
	}




}

/* ==== naming convention ==== */

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
-make save buttons visible when training is done!

TRAIN         -- should this be changed to DATA instead of train?

-band selects/inputs (via TextField) a mood/classifier they want to train, FluCoMa uses the string in Dataset Identifier
-they must be able to add mulitple
-must have dedicated start and stop buttons that end data point entry...should also be gated by Loudness

-be able to write dataSet to json file - must have a big SAVE button! (should they also have the option to clear the archive/start clean?)
-before/instead of SAVE button, maybe there should be an ADD TO DATASET button...in case they record something and then decide afterwards it was trash?
-And can they also change the label retroactively? maybe add the data points to a dataset, then the save/add button executes filling a labelset (it's just ids and labels, time isn't important)

-add classifier to .classifiers + save to the archive... maybe *initClass should populate the classifiers dict?

-how do we associate synth behaviour with classifiers? Is that done manually during/after training? Is it absolute?


-when does "fitting" happen?? should it fit automatically or only on user request?
-automate fitting with a routine...can inform band that if they don't like how it's working they can 1. add more data 2. refit to get different results 3. Hire a new programmer??


PLAY
-check
-should have feedback button (map to spacebar?) that adds a point to dataset
-timed sections, change instruments/intensity/aesthetics

-there needs to be a wet/dry control...is this just a \freq control for the analysis SendReply?

-MLPClassifier runs at .kr and tries to classify behaviour based on available choices... must have some filtering (sample and hold?) to ensure longer phrases and not wild switching
-switch statement a la EIDOLON makes decisions about what kind of processing happens.


*/
