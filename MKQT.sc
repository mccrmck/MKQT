MKQT {

	*new {}


	init {}
}



/*

GUI

-make a startup file that opens a gui window!
-main gui window gives setup choices like devices, i/o info, etc. + 2 buttons: TRAIN & PLAY
-these buttons open separate windows (or layered Views?), each of which has a "back" button as well as a save button!

TRAIN

-band selects/inputs (via TextField) a mood/classifier they want to train, FluCoMa uses the string in Dataset Identifier
-they must be able to add mulitple
-must have dedicated start and stop buttons that end data point entry...should also be gated by Loudness

-
-be able to write dataSet to json file - must have a big SAVE button!

-when does "fitting" happen?? should it fit automatically or only on user request?


PLAY
-check
-should have feedback button (map to spacebar?) that adds a point to dataset
-timed sections, change instruments/intensity/aesthetics

-MLPClassifier runs at .kr and tries to classify behaviour based on available choices... must have some filtering (sample and hold?) to ensure longer phrases and not wild switching
-switch statement a la EIDOLON makes decisions about what kind of processing happens.


*/