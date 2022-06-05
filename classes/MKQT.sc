MKQT {
	classvar <>classifiers;
	classvar <>mainDataSet, <>mainLabelSet;
	classvar <janIn, <floIn, <karlIn, <mstrOut;

	*initClass {

		classifiers = List(); // all classifiers get added to this dictionary and saved?

		ServerTree.add({ |server|                         // check if this makes sense...I think Cmd +. will make new instances, is that good/bad???
			mainDataSet = FluidDataSet(server);
			mainLabelSet = FluidLabelSet(server)},
		\default
		)
	}

	// necessary?
	*new { |janIn, floIn, karlIn, mstrOut|
		^super.new.init(janIn, floIn, karlIn, mstrOut)
	}

	// necessary?
	init { |janIn_, floIn_, karlIn_, mstrOut_|             // maybe I can make Server.default a class arg here? that would save some characters...

		janIn = janIn_;
		floIn = floIn_;
		karlIn = karlIn_;
		mstrOut = mstrOut_;



	}


	/* ==== data collection ==== */

	*dataFromLiveInput {}

	*dataFromBuffer { |pathString, plotAnal = false|

		var server = Server.default;                                        // another server instance
		var path = PathName(pathString);
		var loader = FluidLoadFolder(path);

		// buffers
		var buffer, monoBuf;
		var indicesBuf  = Buffer(server);
		var featuresBuf = Buffer(server);                           // a buffer for writing the analyses into
		var statsBuf    = Buffer(server);                           // a buffer for writing the statistical summary of the analyses into
		var flatBuf     = Buffer(server);                           // a buffer for writing the mean MFCC values into

		var dataset     = FluidDataSet(server);

		Routine({

			//  buffer
			case
			{path.isFile}{ buffer = Buffer.read(server,pathString) }
			{path.isFolder}{ loader.play(server,{ buffer = loader.buffer }) };

			server.sync;

			// make mono
			case
			{ buffer.numChannels == 1 }{ monoBuf = buffer}
			{ buffer.numChannels == 2 }{
				monoBuf = Buffer(server);
				FluidBufCompose.processBlocking(server,buffer,startChan: 0,numChans: 1,gain: -3.dbamp,destination: monoBuf,destGain: 1);
				FluidBufCompose.processBlocking(server,buffer,startChan: 0,numChans: 1,gain: -3.dbamp,destination: monoBuf,destGain: 1);
			}
			{ buffer.numChannels > 2 }{ "multichannel file input not implemented yet - remind Mike".warn };

			server.sync;

			// reduce/remove silences - these numbers are based on 48k but could/should be scaled to reflect sampleRate!
			FluidBufAmpGate.processBlocking(server,monoBuf,
				indices: indicesBuf,
				rampUp: 30,                 // The number of samples the envelope follower will take to reach the next value when raising.
				rampDown: 1200,             // The number of samples the envelope follower will take to reach the next value when falling.
				onThreshold: -18,           // The threshold in dB of the envelope follower to trigger an onset, aka to go ON when in OFF state.
				offThreshold: -32,          // The threshold in dB of the envelope follower to trigger an offset, , aka to go OFF when in ON state.
				minLengthAbove: 480,        // The length in samples that the envelope have to be above the threshold to consider it a valid transition to ON.
				minLengthBelow: 480,        // The length in samples that the envelope have to be below the threshold to consider it a valid transition to OFF.
				lookBack: 480,
				lookAhead: 480,
				action:{ if(plotAnal,{ FluidWaveform(monoBuf, indicesBuf) }) }
			);

			server.sync;

			indicesBuf.loadToFloatArray(action:{ |sliceArray|

				sliceArray.pairsDo({ |startFrame, endFrame, sliceIndex|
					var numFrames = endFrame - startFrame;
					sliceIndex = (sliceIndex / 2).asInteger;

					"analyzing slice: % / %".format(sliceIndex,(sliceArray.size / 2 - 1).asInteger).postln;

					FluidBufSpectralShape.processBlocking(server,monoBuf,startFrame, numFrames,features: featuresBuf, minFreq: 20); // featuresBuf = many frames, 7 chans(descriptors)

					// consider adding weights here...from an FluidBufLoudness maybe?
					FluidBufStats.processBlocking(server,featuresBuf,stats: statsBuf);                       // statsBuf = 49 frames, 7 chans

					// FluidBufCompose.processBlocking() // is it possible to fill the flatBuf with spectralShape ++ stats? Does that make sense?

					FluidBufFlatten.processBlocking(server,statsBuf,numFrames: 2,destination: flatBuf);      // flatBuf = 14 frames, 1 chan

					dataset.addPoint("%-%".format(Date.getDate.format("%M%H%d%m%y"),sliceIndex),flatBuf);    // does this work?
					server.sync;
				});
			});

			[ buffer, monoBuf, indicesBuf, featuresBuf, statsBuf, flatBuf ].do(_.free);

			"\nanalysis done\n".postln

		}).play;

		^dataset
	}

	/* ==== playing methods ==== */

	*makeDataAndLabelSets { |paths| // array from GUI
		var labelId = 0;
		var names = paths.collect({ |p,i| PathName(p).fileNameWithoutExtension });    		// check for file types? If .json.not, throw an error?
		var dSets = paths.collect({ |p,i| FluidDataSet(Server.default).read(p,{ "dataSet: % loaded".format(names[i]).postln }) });

		names = names.collect({ |name| name.split($_)[0] });


		//sort dSets based on file names? and then handle duplicates by merging them?
		// dSets = this.removeDataSetDoubles;


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
