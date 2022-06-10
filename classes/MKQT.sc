MKQT {
	classvar <>classifiers;
	classvar <>mainDataSet, <>mainLabelSet, <>mlp;
	classvar <janIn, <floIn, <karlIn, <mainOut, <server;

	classvar <>classifierIndex, <>prob = 0;
	classvar <>synthLib, <>synthLookup;
	classvar <>verbose = false;

	*initClass {
		Class.initClassTree(Spec);
		Spec.add(\pcMix,ControlSpec(0,1,3,0.001,0.01));

		synthLib = IdentityDictionary();
		classifiers = List(); // all classifiers get added to this dictionary and saved?

		ServerTree.add({ |server|                         // check if this makes sense...I think Cmd+. will make new instances, is that good/bad???
			mainDataSet = FluidDataSet(server);
			mainLabelSet = FluidLabelSet(server);
			mlp = FluidMLPClassifier(server) },
		\default
		)
	}

	*new { |janIn, floIn, karlIn, mainOut, server|
		^super.new.init(janIn, floIn, karlIn, mainOut, server)
	}

	init { |janIn_, floIn_, karlIn_, mainOut_,server_|             // maybe I can make Server.default a class arg here? that would save some characters...
		var path = Platform.userExtensionDir +/+ "MKQT";

		janIn = janIn_;
		floIn = floIn_;
		karlIn = karlIn_;
		mainOut = mainOut_;

		// should I clear synthLib and classifiers?

		server = server_ ? Server.default;

		synthLookup = thisProcess.interpreter.executeFile(path +/+ "synthLookup.scd");
	}

	/* ==== data collection ==== */

	*dataFromLiveInput {}

	*dataFromBuffer { |pathString, analBool = false|

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
				rampUp: 48,                 // The number of samples the envelope follower will take to reach the next value when raising.
				rampDown: 48,             // The number of samples the envelope follower will take to reach the next value when falling.

				// do these two possibly get passed into the function as args?
				onThreshold: -6,           // The threshold in dB of the envelope follower to trigger an onset, aka to go ON when in OFF state.
				offThreshold: -8,          // The threshold in dB of the envelope follower to trigger an offset, , aka to go OFF when in ON state.

				minLengthAbove: 480,        // The length in samples that the envelope have to be above the threshold to consider it a valid transition to ON.
				minLengthBelow: 480,        // The length in samples that the envelope have to be below the threshold to consider it a valid transition to OFF.
				lookBack: 480,
				lookAhead: 480,
				action:{ if(analBool,{ FluidWaveform(monoBuf, indicesBuf) }) }
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

					// FluidBufCompose.processBlocking()                                                     // possible/useeful to send spectralShape ++ stats for BufFlatten?

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
		var dSets = paths.collect({ |p,i| FluidDataSet(server).read(p,{ "dataSet: % loaded".format(names[i]).postln }) });

		names = names.collect({ |name| name.split($_)[0] });


		// sort dSets based on file names? and then handle duplicates by merging them?
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

	*train {
		Routine({
			var dataBool, labelBool;

			mainDataSet.size({ |size| dataBool = size > 0});
			server.sync;
			mainLabelSet.size({ |size| labelBool = size > 0 });
			server.sync;

			if(dataBool and: { labelBool },{
				mlp.fit( mainDataSet, mainLabelSet,{ |loss|
					"loss: %".format(loss).postln;
				});
			},{
				"data: % or labels: % not loaded".format(dataBool, labelBool).postln;
			})
		}).play
	}


	*fillSynthLib {

		classifiers.do({ |class|

		});

	}

	*startPerformance { |performanceDur|

	}
}