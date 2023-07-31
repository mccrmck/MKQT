MKQT2 {

	classvar <analysisFunc;
	classvar <guiAddr;
	var	server;

	*initClass {

		StartUp.add({

			guiAddr = NetAddr("127.0.0.1",8080);

			analysisFunc = {|in|
				var sig      = in.sum;
				var pitch    = FluidPitch.kr(sig); // pitch, confidence
				var mfcc     = FluidMFCC.kr(sig,13,40,1,); // 13 coeffs
				var spectrum = FluidSpectralShape.kr(sig,['centroid', 'spread', 'rolloff', 'flatness']); // centroid, spread, rolloff, flatness
				var loudness = FluidLoudness.kr(sig,[\loudness]); // loudness

				var info = [pitch, mfcc, spectrum, loudness].flat;
				SendReply.kr(Impulse.kr(\trigFreq.kr(0)),'/analysis',info);
				in
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
					mixer.addInsert('analyzer',MKQT2.analysisFunc,{ action.value })
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

	var <>janBus, <>floBus, <>karlBus1, <>karlBus2, <>outBus;
	var janSendBus, floSendBus, karlSendBus;
	var <mlps, <analyBuffer, <synthBuffer;

	var <eqBusses, <compCrossO, <compBusses;

	var	server, medianFilt;
	var <mixer;

	*new { |janIn, floIn, karlIn1, karlIn2, out|                            // gotta ensure these get turned into arrays from o-s-c
		^super.newCopyArgs(janIn, floIn, karlIn1, karlIn2, out).init
	}

	init {
		server = Server.default;

		server.quit;
		server.waitForBoot({

			mlps        = ( jan: FluidMLPRegressor(server), flo: FluidMLPRegressor(server), karl: FluidMLPRegressor(server) );
			analyBuffer = Buffer.alloc(server,19);
			synthBuffer = Buffer.alloc(server,10);
			medianFilt  = Array.fill(30,{ 0 });       // this should be a one-second filter over data values...30 slots for 30Hz trigfreq

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
		})
	}

	startPerformance { |waitForStart|

		Routine{
			waitForStart.wait;
			this.startAnalysis;
			this.loadPredictOSCdef;
		}.play
	}

	stopPerformance {
		this.stopAnalysis;
		this.freePredictOSCdef;
	}

	makeMixer {
		janSendBus  = Bus.audio(server,2);
		floSendBus  = Bus.audio(server,2);
		karlSendBus = Bus.audio(server,2);

		fork {
			mixer = Mixer('mkqtMain',['jan','flo','karl'],outBus,server,{
				this.addInsertsAndSends('jan',janBus,janSendBus);
				this.addInsertsAndSends('flo',floBus,floSendBus);
				this.addInsertsAndSends('karl',[karlBus1,karlBus2],karlSendBus);
			});
			this.addMixerInserts;
		}
	}

	addInsertsAndSends { |playerKey, inBus, sendBus|
		var triggerKey = "%Trigger".format(playerKey).asSymbol;

		if(inBus.isArray,{
			mixer[playerKey].addInSynth({ SoundIn.ar(\inBus1.kr( inBus[0] )) + SoundIn.ar(\inBus2.kr( inBus[1] )) },{
				mixer[playerKey].addInsert(triggerKey,this.prTriggerSynth(triggerKey),{
					mixer[playerKey].addPreFaderSend('fx', sendBus,{ |sendSynth| sendSynth.set(\amp,1) })
				});
			});
		},{
			mixer[playerKey].addInSynth({ SoundIn.ar(\inBus.kr( inBus ))!2 },{
				mixer[playerKey].addInsert(triggerKey,this.prTriggerSynth(triggerKey),{
					mixer[playerKey].addPreFaderSend('fx', sendBus,{ |sendSynth| sendSynth.set(\amp,1) })
				});

			});
		});
		mixer.addSendStrip(playerKey,{ |in| in /* dummy */ },{ |fader| fader.set(\amp,1) });
	}

	prTriggerSynth { |key|
		^{ |in|
			var onsets = FluidOnsetSlice.ar(in.sum,9,\onsetThresh.kr(0.5));
			SendReply.ar(onsets,"/%".format(key).asSymbol,[onsets]);
			in
		}
	}

	addMixerInserts {
		mixer.addInsert('analyzer',MKQT2.analysisFunc,{ });

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

			medianFilt = medianFilt.rotate(1).put(0,data);
			data = medianFilt.flop.collect({ |i| i.median });

			analyBuffer.setn(0,data)

		},'/analysis');

		mixer.inserts['analyzer'].set(\trigFreq,30)
	}

	stopAnalysis {
		OSCdef(\parseAnalysis).free;
		mixer.inserts['analyzer'].set(\trigFreq,0)
	}

	loadPredictOSCdef {

		['jan','flo','karl'].do({ |key,index|
			var sendBus = [janSendBus,floSendBus,karlSendBus][index];

			OSCdef("%Trigger".format(key).asSymbol,{ |msg|

				if( 0.2.coin,{
					mlps[key].predictPoint(analyBuffer,synthBuffer,{
						synthBuffer.getn(0,10,action:{ |array|

							MKQTSynth(sendBus,mixer.sends[key].stripBus,mixer.sends[key].inGroup,*array)
						});
					});
				});

			},"/%Trigger".format(key).asSymbol)
		})
	}

	freePredictOSCdefs {
		['jan','flo','karl'].do({ |key,index|

			OSCdef("/%Trigger".format(key).asSymbol).free
		})
	}

	sendOSCVisuals { |ip, port, delta = 0.1|
		var addr = NetAddr(ip,port);
		Routine({
			loop{
				delta.wait;
				analyBuffer.getn(0,19,{ |array|
					// addr.sendMsg("/address",*array)    // need to know what IP, what port, and which osc address
				})
			}
		})
	}


}