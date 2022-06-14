MKQT {
	classvar <>classifiers;
	classvar <>mainDataSet, <>mainLabelSet, <>mlp;
	classvar <janIn, <floIn, <karlIn, <mainOut, <server;
	classvar <fxSends, <ampBus;

	classvar <>classifierIndex = 0, <>prob = 0.01;
	classvar <>synthLib, <>synthLookup, <synthOSCfuncs;
	classvar <>verbose = false;

	*initClass {
		Class.initClassTree(Spec);
		Spec.add(\pcMix, ControlSpec(0,1,3,0.001,0.01));

		synthLib = IdentityDictionary();
		classifiers = List(); // all classifiers get added to this dictionary and saved?

		ServerTree.add({ |server|                         // check if this makes sense...I think Cmd+. will make new instances, is that good/bad???
			mainDataSet = FluidDataSet(server);
			mainLabelSet = FluidLabelSet(server);
			mlp = FluidMLPClassifier(server,[11,8],activation:1,learnRate: 0.05,momentum: 0.3 ) },
		\default
		)
	}

	*new { |janIn, floIn, karlIn, mainOut, server|
		^super.new.init(janIn, floIn, karlIn, mainOut, server)
	}

	init { |janIn_, floIn_, karlIn_, mainOut_,server_|             // maybe I can make Server.default a class arg here? that would save some characters...
		var path = Platform.userExtensionDir +/+ "MKQT";
		server = server_ ? Server.default;

		janIn = janIn_;
		floIn = floIn_;
		karlIn = karlIn_;
		mainOut = mainOut_;
		fxSends = Array.fill(3,{ Bus.audio(server) });
		ampBus = Bus.control(server);

		// should I clear synthLib and classifiers?

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
				rampDown: 192,             // The number of samples the envelope follower will take to reach the next value when falling.

				// do these two possibly get passed into the function as args?
				onThreshold: -9,           // The threshold in dB of the envelope follower to trigger an onset, aka to go ON when in OFF state.
				offThreshold: -12,          // The threshold in dB of the envelope follower to trigger an offset, , aka to go OFF when in ON state.

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

	*addSynths { |list = false|
		var path = Platform.userExtensionDir +/+ "MKQT";
		thisProcess.interpreter.executeFile(path +/+ "addSynths.scd").value(list);
	}

	*makeDataAndLabelSets { |paths| // array from GUI
		var strings = paths.collect({ |p,i| PathName(p).fileNameWithoutExtension });    		// check for file types? If .json.not, throw an error?
		var dSets = paths.collect({ |p,i| FluidDataSet(server).read(p,{ "dataSet: % loaded".format(strings[i][0]).postln }) });
		strings = strings.collect({ |name| name.split($_) });

		// sort dSets based on file names? and then handle duplicates by merging them?
		// dSets = this.removeDataSetDoubles;

		// build the labelSet
		strings.do({ |string,index|
			var name = string[0];
			var stamp = string[1];

			classifiers.add(name.asSymbol);

			dSets[index].size({ |size|

				size.do({ |i|
					this.mainLabelSet.addLabel("%-%".format(stamp,i),name.asString);  // not sure I need "this"
				});
			})
		});

		// build the dataSet
		this.mainDataSet = this.concatDataSets(dSets); // not sure I need the first "this"
	}

	*concatDataSets { |dataSets|  // must be sorted by label/classifier first...which would be fileName in gui!
		var zeroSet = dataSets[0];

		dataSets[1..].do({ |dSet|
			zeroSet.merge(dSet)
		});
		^zeroSet
	}

	*getLabels {
		mlp.dump({ |data|
			data["labels"]["labels"].do({ |name|
				classifiers.add(name.asSymbol);
			})
		});
	}

	*train {
		// add something here to make sure dataset and labelset are populated
		mlp.fit( mainDataSet, mainLabelSet,{ |loss|
			"loss: %".format(loss).postln;
		});

	}

	*fillSynthLib {

		if(classifiers.isEmpty,{
			"no classifiers loaded".postln
		},{
			classifiers.do({ |class|
				var array = synthLookup[class];

				synthLib.put(class.asSymbol, array.scramble)
			});
		});
	}

	*playerNdefs {

		['Jan','Flo','Karl'].do({ |name, index|
			var key = ("mkqtIn" ++ name).asSymbol;
			var sendAddr = ("/mkqtRespond" ++ name).asSymbol;
			var inBus = [ MKQT.janIn, MKQT.floIn, MKQT.karlIn ].at(index);
			var sendBus = MKQT.fxSends[index];

			Ndef(key,{                                               // compression may not be necessary pga. digital instruments...
				var sig = SoundIn.ar(inBus);
				sig = Mix(sig);
				// sig = Compander.ar(sig,sig,\compThresh.kr(0.5),1,\compRatio.kr(2).reciprocal,\compAtk.kr(0.01),\compRls.kr(0.1),\muGain.kr(2));
				Out.ar(sendBus,sig);
				Pan2.ar(sig,\pan.kr(0));
			}).play(out: MKQT.mainOut);

			Ndef(key).filter(1,{ |in|
				var sig = in.sum * -3.dbamp;
				var onsets = FluidOnsetSlice.ar(sig,threshold: \onsetThresh.kr(0.5)); // go through these, do some testing if possible???
				var specChange = FluidNoveltySlice.ar(sig,0,11,\noveltyThresh.kr(0.33)); // must check this - should algorithm be 1?

				SendReply.ar(onsets + specChange,sendAddr,[onsets,specChange]);
				in * \amp.kr(0);
			})
		})
	}

	*startPerformance { |performanceDur|

		this.startPerformanceClock( performanceDur );
		this.makeMainDefs;
		this.makePlayerOSCdefs;
	}

	*startPerformanceClock { |performanceDur|

		Ndef(\clock,{
			var trig = Impulse.kr(1);
			var time = Sweep.ar(\reset.tr(1));
			SendReply.kr(trig,'/fibonacci',[ time ]);
		});

		OSCdef(\clockListener,{ |msg, time, addr, recvPort|
			var timeSinceStart = msg[3].round(1);
			var fibArray = Array.fib(9,1,1) * 60;
			// timeSinceStart.postln;

			if(timeSinceStart <= performanceDur,{
				fibArray.do({ |fibNum|
					if(timeSinceStart == fibNum,{
						MKQT.synthLib.keysValuesChange({ |key,synthKeys|
							synthKeys.rotate(-1);
						});
						"time: % \nsynths rotated".format(timeSinceStart).postln;
					})
				});
			},{
				Ndef(\mkqtPredict).clear;
				OSCdef(\mkqtPredictOSC).clear;
				synthOSCfuncs.do(_.free);
				"performance over".postln;
				// this.stopPerformance  // make something better here - send message to GUI
			});
		},'/fibonacci')
	}

	*makeMainDefs {
		Ndef(\mkqtPredict,{
			var trigGate, trig;
			var statsBuf = LocalBuf(14);
			var outBuf = LocalBuf(1);
			var sig = SoundIn.ar(janIn) + SoundIn.ar(floIn) + SoundIn.ar(karlIn);
			sig = Mix(sig);
			trigGate = ( FluidLoudness.kr(sig)[0] > \trigGateThresh.kr(-18) );
			trig = Impulse.kr(\trigRate.kr(10) * trigGate);
			sig = FluidSpectralShape.kr(sig, minFreq: 20);
			sig = FluidStats.kr(sig,20).flat; // should experiment with history window size!!

			FluidKrToBuf.kr(sig,statsBuf);
			mlp.kr(trig, statsBuf, outBuf); // has to be the same instance that was trained!!
			sig = FluidBufToKr.kr(outBuf);

			SendReply.kr(trig,'/mkqtPredict',[ sig ]);
		});

		OSCdef(\mkqtPredictOSC,{ |msg, time, addr, recvPort|
			var classIndex = msg[3];                                                        // add a median filter here ?!?!?!
			MKQT.classifierIndex = classIndex;

			if(MKQT.verbose,{ MKQT.classifiers[ classIndex ].postln })
		},'/mkqtPredict')
	}

	*makePlayerOSCdefs {

		synthOSCfuncs =  ['Jan','Flo','Karl'].collect({ |name, index|
			var nDefKey = ("mkqtIn" ++ name).asSymbol;
			var rcvAddr = ("/mkqtRespond" ++ name).asSymbol;
			var inBus = MKQT.fxSends[index];

			OSCFunc({ |msg, time, addr, recvPort|
				var onsetTrig =  msg[3];
				var specTrig =  msg[4];

				// [onsetTrig,specTrig].postln;

				if( MKQT.prob.coin,{
					var classKey = MKQT.classifiers[ MKQT.classifierIndex ];
					var synthKey = MKQT.synthLib[ classKey.asSymbol ][0];

					var defaultArgs = [
						\inBus,inBus,
						\pan, 0,                                                      // what do I do with this??? Add randomness? Map to an LFO?
						\out,MKQT.mainOut,
					];

					var envArgs, envParts, shape, curve;
					var envStyle = [\perc,\sustain].choose; // what else? Step? lopsided fades?

					var uniqueArgs = SynthDescLib.global[synthKey].controlNames.reject({ |cName|
						cName == 'inBus' || { cName == 'pan' } || { cName == 'amp' } || { cName == 'out' } ||
						{ cName == 'atk' } || { cName == 'rls' } || { cName == 'shape' } || { cName == 'curve' }
					});

					uniqueArgs = uniqueArgs.collect({ |key| [ key, 1.0.rand ] }).flat;

					case
					{ envStyle == \perc }{
						envParts = [0.01,2.rrand(5.0)].scramble;
						if(envParts[0] < envParts[1],{
							shape = Env.shapeNumber(-4);
							curve = -4;
						},{
							shape = Env.shapeNumber(4);
							curve = 4;
						})
					}
					{ envStyle == \sustain }{
						envParts = 4.rrand(8.0)!2;
						shape = Env.shapeNumber(\welch);
						curve = 0;
					};

					envArgs = [
						\atk, envParts[0],
						\rls, envParts[1],
						\shape, shape,
						\curve, curve,
					];

					// this can also be 4.do({ Synth instance w/ randArgs }), especially if they are grain-ish processes
					Synth(synthKey,defaultArgs ++ envArgs ++ uniqueArgs, Ndef(nDefKey).group, \addAfter).map(\amp,MKQT.ampBus);

					if(verbose,{ "%:%".format(synthKey,name).postln })
				});
			},rcvAddr)
		})
	}

	* stopPerformance {}

	*cleanUp {
		// free buffers
		// free OSCFuncs
		// other resources???
	}
}