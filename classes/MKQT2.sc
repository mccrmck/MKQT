MKQT2 {

	classvar <analysisFunc;
	classvar <guiAddr;
	var	server;

	*initClass {

		StartUp.add({

			guiAddr = NetAddr("127.0.0.1",8080);

			analysisFunc = { |oscKey|
				{|in|
					var sig      = in.sum;
					var pitch    = FluidPitch.kr(sig); // pitch, confidence
					var mfcc     = FluidMFCC.kr(sig,13,40,1,); // 13 coeffs
					var spectrum = FluidSpectralShape.kr(sig,['centroid', 'spread', 'rolloff', 'flatness']); // centroid, spread, rolloff, flatness
					var loudness = FluidLoudness.kr(sig,[\loudness]); // loudness

					var info = [pitch, mfcc, spectrum, loudness].flat;
					SendReply.kr(Impulse.kr(\trigFreq.kr(0)),oscKey,info);
					in
				}
			};
		})
	}

	*new {
		^super.new.init
	}

	init {
		server = Server.default;
		this.openStageControl;
		this.guiOSCdefs

	}

	openStageControl {
		var unixString = "open /Applications/open-stage-control.app --args " ++
		"--send 127.0.0.1:% ".format( NetAddr.localAddr.port ) ++
		"--load '%'".format("/Users/mikemccormick/Library/Application Support/SuperCollider/Extensions/MKQT/main.json");                                   // this path needs to eventually be sorted out...

		unixString.unixCmd
	}
}


MKQTTrain {

	// these variables seem very chaotic!! FIX
	var <mlp;
	var analyDataSet, synthDataSet;
	var analyBuffer, synthBuffer;
	var dataArray;
	var server, <mixer;
	var <pbSynth;
	var pointID;
	var fxRoutine, fxSendBus;

	*initClass {}

	*new {
		^super.new.init
	}

	init {
		server = Server.default;
		pointID = 0;

		server.quit;
		server.waitForBoot({

			mlp          = FluidMLPRegressor(server, [15,12], FluidMLPRegressor.relu, FluidMLPRegressor.relu,learnRate: 0.01);  // fix layer structure...?
			analyDataSet = FluidDataSet(server);
			synthDataSet = FluidDataSet(server);
			analyBuffer  = Buffer.alloc(server,19);
			synthBuffer  = Buffer.alloc(server,10);
			dataArray    = Array.fill(19,{0});
		});
	}

	makeMixer { |bufferPath, action|
		var buffer = Buffer.cueSoundFile(server,bufferPath,bufferSize: 65536);
		fxSendBus  = Bus.audio(server,2);

		mixer = Mixer('mkqtTrain',['playback'],[0,1],server,{
			mixer['playback'].addInSynth({
				VDiskIn.ar(2,buffer.bufnum,\rate.kr(0));
			},{
				mixer['playback'].addPreFaderSend('fx',fxSendBus,{ |sendSynth|
					sendSynth.set(\amp,1);
					mixer.addInsert('analyzer',MKQT2.analysisFunc.('/analysis'),{ action.value })
				})
			})
		});
	}

	changeBuffer { |bufferPath|
		var buffer = Buffer.cueSoundFile(server,bufferPath,bufferSize: 65536);

		mixer['playback'].inSynth.free;
		mixer['playback'].addInSynth({
			VDiskIn.ar(2,buffer.bufnum,\rate.kr(0));
		})
	}

	startPlayback {
		mixer['playback'].amp = 1;
		mixer.inserts['analyzer'].set(\trigFreq,30);
		mixer.amp = 1;
		mixer['playback'].inSynth.set(\rate,1);
		this.startFXsynths;
		this.startAnalysis;
	}

	stopPlayback {
		mixer['playback'].amp = 0;
		mixer['playback'].inSynth.set(\rate,0);
		mixer.inserts['analyzer'].set(\trigFreq,0);
		mixer.amp = 0;
		this.stopFXsynths;
		OSCdef(\parseAnalysis).free
	}

	startFXsynths {
		fxRoutine =	Routine{
			loop{
				var array = Array.fill(10,{1.0.rand});
				(array[6] + array[7]).linlin(0,1,0.01,8).wait;
				synthBuffer.setn(0,array);
				analyBuffer.setn(0,dataArray);
				MKQTSynth(fxSendBus, mixer.mixerBus, mixer.sendGroups[0], *array)
			}

		}.play
	}

	stopFXsynths {
		fxRoutine.stop
	}

	startAnalysis {

		OSCdef(\parseAnalysis,{ |msg|
			var pitch    = msg[3].explin(20,20000,0,1);
			var conf     = msg[4];
			var mfcc     = msg[5..17].linlin(-250,250,0,1);
			var centroid = msg[18].explin(20,20000,0,1);
			var spread   = msg[19].explin(20,20000,0,1);
			var rolloff  = msg[20].explin(20,20000,0,1);
			var flatness = \db.asSpec.unmap(msg[21]);
			var loudness = \db.asSpec.unmap(msg[22]);

			var data = [pitch, conf, mfcc, centroid, rolloff, flatness, loudness].flat;

			dataArray = data;

		},'/analysis');
	}

	addPointToDataSet {
		analyDataSet.addPoint(pointID,analyBuffer);
		synthDataSet.addPoint(pointID,synthBuffer);
		pointID = pointID + 1;

		analyDataSet.dump;
		synthDataSet.dump;
	}

	train {
		mlp.fit(analyDataSet,synthDataSet,{ |loss| "loss: %".format(loss).postln })
	}

	saveDataSets { |path|
		var date = Date.getDate.stamp;
		var pathName = PathName(path);
		var fileName = pathName.fileNameWithoutExtension;

		analyDataSet.write(pathName.pathOnly +/+ date ++ fileName ++ "_analyDS.json");
		synthDataSet.write(pathName.pathOnly +/+ date ++ fileName ++ "_synthDS.json");
		"%_analysisDS.json saved".format(fileName);
		"%_synthDS.json saved".format(fileName);
	}

	saveMlp { |path|
		var date = Date.getDate.stamp;
		var fileName = PathName(path).fileName;
		var pathName = PathName(path).pathOnly;

		mlp.write(pathName +/+ date ++ fileName);
		"mlp: % saved".format(fileName)
	}

	clearMixer {
		mixer.free
	}
}


MKQTPlay {

	// clean all this up, it's disgusting!

	var <>janBus, <>floBus, <>karlBus, <>outBus;
	var janSendBus, floSendBus, karlSendBus;
	var <mlps, <analyBuffer, <synthBuffer, visualBuffer;

	var <eqBusses, <compCrossO, <compBusses;

	var	server, mlpMedianFilt, visualMedianFilt;
	var <mixer, visuals;

	*new { |janIn, floIn, karlIn, out|                            // gotta ensure these get turned into arrays from o-s-c
		^super.newCopyArgs(janIn, floIn, karlIn, out).init
	}

	init {
		server = Server.default;

		server.quit;
		server.waitForBoot({

			mlps         = ( jan: FluidMLPRegressor(server), flo: FluidMLPRegressor(server), karl: FluidMLPRegressor(server) );
			analyBuffer  = Buffer.alloc(server,19);
			synthBuffer  = Buffer.alloc(server,10);
			visualBuffer = Buffer.alloc(server,19);

			mlpMedianFilt = Array.fill(15,{ 0 });       //  / 30 for time in seconds
			visualMedianFilt  = Array.fill(15,{ 0 });   //  / 30 for time in seconds

			eqBusses = (
				freq:  Bus.control(server,5).setn([40,160,640,1280,5120]),
				rq:    Bus.control(server,5).setn(0!5),
				db:    Bus.control(server,5).setn(0!5),
			);
			compCrossO = Bus.control(server,3).setn([150,550,3000]);
			compBusses = (
				thresh: Bus.control(server,4).setn(0.dbamp!4),
				atk:    Bus.control(server,4).setn(0.01!4),
				rls:    Bus.control(server,4).setn(0.01!4),
				ratio:  Bus.control(server,4).setn(4!4),
				knee:   Bus.control(server,4).setn(0!4),
				muGain: Bus.control(server,4).setn(0.dbamp!4),
			);
			this.makeMixer
		})
	}

	startPerformance { |waitForStart|
		var waitTime = waitForStart ? 0;

		Routine{
			waitTime.wait;
			this.startAnalysis;
			this.loadPredictOSCdefs;
		}.play
	}

	stopPerformance {
		this.stopAnalysis;
		this.freePredictOSCdefs;
	}

	makeMixer {
		var cond = CondVar();
		janSendBus  = Bus.audio(server,2);
		floSendBus  = Bus.audio(server,2);
		karlSendBus = Bus.audio(server,2);

		fork {
			var run = 0;
			mixer = Mixer('mkqtMain',['jan','flo','karl'],outBus,server,{ cond.signalOne });
			cond.wait { mixer.fader.notNil };
			this.addSendStrips({ run = run + 1; cond.signalOne });
			cond.wait{ run == 3 };
			run = 0;
			this.addSends({ run = run + 1; cond.signalOne });
			cond.wait{ run == 3 };
			run = 0;
			this.addInSynths({ run = run + 1; cond.signalOne });
			cond.wait{ run == 3 };
			run = 0;
			this.addMixerInserts({ run = run + 1; cond.signalOne });
			cond.wait{ run == 2 };
			run = 0;
			this.loadMLPs;
		};
	}

	order {
		// ensure MKQTSynths are going to the right place!
		// ensure that setting \trigFreq is being directed at the right synths!
	}

	addSendStrips {|action|
		mixer.addSendStrip('analyzer',MKQT2.analysisFunc.('/analysis'),{                 // global analyzer - depth 1
			mixer.addSendStrip('visualizer',MKQT2.analysisFunc.('/visuals'),{            // fx analyzer for visuals - depth 1
				mixer.strips.keysDo({ |key|                                              // fxStrips(3) for each mixerStrip - depth 0
					mixer.addSendStrip("%FX".format(key).asSymbol,{ |in| in /* dummy */ },{
						action.value;
					},0);
				});
			},1);
		},1);
	}

	addSends { |action|
		mixer.strips.keysDo({ |key|
			mixer[key].addPreFaderSend("%Analy".format(key).asSymbol,mixer.sends['analyzer'].stripBus,{ |analySend|                                          // preFSend(3) inSynth -> global Analyzer
				var fxKey = "%FX".format(key).asSymbol;
				analySend.set(\amp,1);
				mixer[key].addPreFaderSend(fxKey, mixer.sends[fxKey].stripBus,{ |fxSend|                           // preFSend(3) inSynth -> fx Strip
					fxSend.set(\amp,1);
					mixer.sends[fxKey].addPreFaderSend("%Visual".format(key).asSymbol,mixer.sends['visualizer'].stripBus,{ |visualSend| // preFSend(3) fx -> visuals
						visualSend.set(\amp,1);
						action.value;
					});
				});
			});
		});
	}

	addInSynths { |action|
		mixer.strips.keysDo({ |key|
			var triggerKey = "%Trigger".format(key).asSymbol;
			mixer[key].addInsert(triggerKey,this.prTriggerSynth(triggerKey),{
				var inBus = (jan: janBus, flo: floBus, karl: karlBus);                    // this seems dumb, no?
				inBus = inBus[key];
				if(inBus.isArray.not,{ inBus = inBus!2 });
				mixer[key].addInSynth({ SoundIn.ar(\inBus.kr( inBus )) },{ action.value });
			});
		});
	}

	prTriggerSynth { |key|
		^{ |in|
			var onsets = FluidOnsetSlice.ar(in.sum,9,\onsetThresh.kr(0.5));
			var novelty = FluidNoveltySlice.ar(in.sum,1,31,\noveltyThresh.kr(0.33));   // what does this even mean? How to calibrate?
			SendReply.ar(onsets + novelty,"/%".format(key).asSymbol,[onsets]);
			in
		}
	}

	addMixerInserts { |action|

		mixer.addInsert('5BandEq',{ |in|
			var cutoffs = In.kr(\freqs.kr(),5);
			var rqs     = In.kr(\res.kr(),5);
			var dBs     = In.kr(\cutBoost.kr(),5);
			var sig     = in;

			5.do({ |i|
				sig = BPeakEQ.ar(sig,cutoffs[i].clip(20,20000),rqs[i].linlin(0,1,2.sqrt,0.01),dBs[i].clip(-24,24))
			});
			sig
		},{ |insertSynth|
			insertSynth.set(
				\freqs,   eqBusses['freq'],
				\res,     eqBusses['rq'],
				\cutBoost,eqBusses['db'],
			);
			action.value;
		});

		mixer.addInsert('4BandComp',{ |in|
			var crossO = In.kr(\crossO.kr(),3);
			var thresh = In.kr(\thresh.kr(),4);
			var atk    = In.kr(\compAtk.kr(),4);
			var rls    = In.kr(\compRls.kr(),4);
			var ratio  = In.kr(\ratio.kr(),4);
			var knee   = In.kr(\knee.kr(),4);
			var muGain = In.kr(\muGain.kr(),4);

			var sig = BandSplitter4.ar(in,crossO[0],crossO[1],crossO[2]);

			sig = sig.collect({ |band, i|
				Squish.ar(band,band,thresh[i],atk[i],rls[i],ratio[i],knee[i],muGain[i]).tanh;
			});
			sig.sum
		},{ |insertSynth|
			insertSynth.set(
				\crossO, compCrossO,
				\thresh, compBusses['thresh'],
				\compAtk,compBusses['atk'],
				\compRls,compBusses['rls'],
				\ratio,  compBusses['ratio'],
				\knee,   compBusses['knee'],
				\muGain, compBusses['muGain'],
			);
			action.value;
		});
	}

	clearMixer {
		mixer.free
	}

	loadMLPs {
		var path = "/Users/mikemccormick/Library/Application Support/SuperCollider/Extensions/MKQT/MLPs/";                // fix this

		['jan','flo','karl'].do({ |key,index|

			mlps[key].read(path ++ "/%.json".format(key),{ "MLP: % loaded".format(key).postln });
		})
	}

	startAnalysis {
		OSCdef(\parseAnalysis,{ |msg|
			var pitch    = msg[3].explin(20,20000,0,1);
			var conf     = msg[4];
			var mfcc     = msg[5..17].linlin(-250,250,0,1);
			var centroid = msg[18].explin(20,20000,0,1);
			var spread   = msg[19].explin(20,20000,0,1);
			var rolloff  = msg[20].explin(20,20000,0,1);
			var flatness = \db.asSpec.unmap(msg[21]);
			var loudness = \db.asSpec.unmap(msg[22]);
			var data = [pitch, conf, mfcc, centroid, rolloff, flatness, loudness].flat;
			mlpMedianFilt = mlpMedianFilt.rotate(1).put(0,data);
			data = mlpMedianFilt.flop.collect({ |i| i.median });

			analyBuffer.setn(0,data)

		},'/analysis');

		mixer.sends['analyzer'].inserts['analyzer'].set(\trigFreq,30)
	}

	stopAnalysis {
		OSCdef(\parseAnalysis).free;
		mixer.sends['analyzer'].inserts['analyzer'].set(\trigFreq,0)
	}

	loadPredictOSCdefs {

		['jan','flo','karl'].do({ |key,index|
			var sendBus = [janSendBus,floSendBus,karlSendBus][index];

			OSCdef("%Trigger".format(key).asSymbol,{ |msg|

				if( 0.2.coin,{
					mlps[key].predictPoint(analyBuffer,synthBuffer,{
						synthBuffer.getn(0,10,action:{ |array|
							// "%: %".format(key,array).postln;
							MKQTSynth(sendBus,mixer.sends["%FX".format(key).asSymbol].stripBus,mixer.sends["%FX".format(key).asSymbol].inGroup,*array)
						});
					});
				});

			},"/%Trigger".format(key).asSymbol)
		})
	}

	freePredictOSCdefs {
		['jan','flo','karl'].do({ |key,index|

			OSCdef("%Trigger".format(key).asSymbol).free
		})
	}

	startOSCVisuals { |ip, port, delta = 0.1|
		var addr = NetAddr(ip,port);
		var local = NetAddr.localAddr;
		visuals = Routine({
			loop{
				delta.wait;
				analyBuffer.getn(0,19,{ |array|
					array = array * 0.2;                  // scaled down to suit the visual speed
					addr.sendMsg("/mkqt",*array);
					local.sendMsg("/write",*array)

				})
			}
		}).play;

		OSCdef(\visualAnalysis,{ |msg|
			var pitch    = msg[3].explin(20,20000,0,1);
			var conf     = msg[4];
			var mfcc     = msg[5..17].linlin(-250,250,0,1);
			var centroid = msg[18].explin(20,20000,0,1);
			var spread   = msg[19].explin(20,20000,0,1);
			var rolloff  = msg[20].explin(20,20000,0,1);
			var flatness = \db.asSpec.unmap(msg[21]);
			var loudness = \db.asSpec.unmap(msg[22]);
			var data = [pitch, conf, mfcc, centroid, rolloff, flatness, loudness].flat;

			visualMedianFilt = visualMedianFilt.rotate(1).put(0,data);
			data = visualMedianFilt.flop.collect({ |i| i.median });

			visualBuffer.setn(0,data)

		},'/visuals');
	}

	stopOSCVisuals {
		visuals.stop;
		OSCdef(\visualAnalysis).free;
	}
}