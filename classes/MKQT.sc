MKQT {
	classvar classifiers;

	*initClass {

		classifiers = IdentityDictionary(); // all classifiers get added to this dictionary and saved?

	}

	*new {
		^super.new.init
	}


	init {}

	*liveTrain {
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

	*bufTrain {

		// things that have to happen here:
		// store classifier name in classifiers Dict, eventually store dict in Archive...maybe write it immediately?


	}

}

/*

GUI
-these buttons open separate windows (or layered Views?), each of which has a "back" button as well as a save button!

TRAIN

-band selects/inputs (via TextField) a mood/classifier they want to train, FluCoMa uses the string in Dataset Identifier
-they must be able to add mulitple
-must have dedicated start and stop buttons that end data point entry...should also be gated by Loudness

-be able to write dataSet to json file - must have a big SAVE button! (should they also have the option to clear the archive/start clean?)
-before/instead of SAVE button, maybe there should be an ADD TO DATASET button...in case they record something and then decide afterwards it was trash?
-And can they also change the label retroactively? maybe add the data points to a dataset, then the save/add button executes filling a labelset (it's just ids and labels, time isn't important)

-add classifier to .classifiers + save to the archive... maybe *initClass should populate the classifiers dict?

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
