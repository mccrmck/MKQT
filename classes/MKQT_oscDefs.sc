+MKQTv2{

	guiOSCdefs {
		var janIn, floIn, karlIn, mainOut;

		OSCdef(\initGUI,{

			MKQTv2.guiAddr.sendMsg("/inDeviceList", *ServerOptions.inDevices);
			MKQTv2.guiAddr.sendMsg("/outDeviceList",*ServerOptions.outDevices)

		},'/guiLoaded');

		OSCdef(\inDevice,{ |msg|
			var val = msg[1].asString;

			Server.default.options.inDevice = val;
			"Audio input device: %".format(val).postln;

		},'/inputDevice');

		OSCdef(\outDevice,{ |msg|
			var val = msg[1].asString;

			Server.default.options.outDevice = val;
			"Audio output device: %".format(val).postln;

		},'/outputDevice');

		OSCdef(\janInBus,{ |msg|
			var val = msg[1] - 1;

			janIn = val

		},'/janIn');

		OSCdef(\floInBus,{ |msg|
			var val = msg[1] - 1;

			floIn = val

		},'/floIn');

		OSCdef(\karlIn,{ |msg|
			var val = msg[1].asString.split($/);
			val = val.collect({ |i| i.interpret - 1 });
			karlIn = val

		},'/karlIn');

		OSCdef(\sampleRate,{ |msg|
			var val = msg[1];
			Server.default.options.sampleRate = val;
		},'/sampleRate');

		OSCdef(\mainOut,{ |msg|
			var val = msg[1].asString.split($/);
			val = val.collect({ |i| i.interpret - 1 });
			mainOut = val;
		},'/mainOut');

		OSCdef(\trainButton,{ |msg|
			var train = MKQTTrain();
			MKQTv2.trainOSCdefs(train);

		},'/train');

		OSCdef(\playButton,{ |msg|
			var jan = janIn ? 0;
			var flo = floIn ? 1;
			var karl = karlIn ? [4,5];

			var play = MKQTPlay(jan, flo, karl, mainOut);
			MKQTv2.playOSCdefs(play)
		},'/play');
	}

	*trainOSCdefs { |instance|
		var train = instance;

		OSCdef(\loadBuffer,{ |msg|
			var path = msg[1].asString;

			if( train.mixer.notNil,{
				train.changeBuffer(path)
			},{
				train.makeMixer(path);
			})

		},'/loadBuffer');

		OSCdef(\playback,{ |msg|
			var val = msg[1];

			if( val == 1,{
				train.startPlayback
			},{
				train.stopPlayback
			})

		},'/playback');

		OSCdef(\saveDataSets,{ |msg|
			var path = msg[1].asString;

			train.saveDataSets(path);

		},'/saveDataSets');

		OSCdef(\exportMlp,{ |msg|
			var path = msg[1].asString;

			train.saveMlp(path);

		},'/saveMlp');

		OSCdef(\train,{ |msg|

			train.train

		},'/trainMlp');

		OSCdef(\addPoint,{ |msg|

			train.addPointToDataSet

		},'/addPoint');

		/* ======= faders/mixer ======= */

		OSCdef(\fxAmp,{ |msg|
			var val = msg[1];
			val = \db.asSpec.map(val).dbamp;
			train.mixer['playback'].preSends['fx'].set(\amp,val)
		},'/effectsAmp');

		OSCdef(\pbAmp,{ |msg|
			var val = msg[1];
			val = \db.asSpec.map(val).dbamp;
			train.mixer['playback'].amp = val
		},'/playbackAmp');
	}

	*playOSCdefs { |instance|
		var play = instance;

		/* ======= calibration ======= */

		OSCdef(\janNoveltyThresh,{ |msg|
			var val = msg[1];
			play.mixer['jan'].inserts['janTrigger'].set(\noveltyThresh,val);
		},'/noveltyThreshJan');

		OSCdef(\janOnsetThresh,{ |msg|
			var val = msg[1];
			play.mixer['jan'].inserts['janTrigger'].set(\onsetThresh,val);
		},'/onsetThreshJan');

		OSCdef(\floNoveltyThresh,{ |msg|
			var val = msg[1];
			play.mixer['flo'].inserts['floTrigger'].set(\noveltyThresh,val);
		},'/noveltyThreshFlo');

		OSCdef(\floOnsetThresh,{ |msg|
			var val = msg[1];
			play.mixer['flo'].inserts['floTrigger'].set(\onsetThresh,val);
		},'/onsetThreshFlo');

		OSCdef(\karlNoveltyThresh,{ |msg|
			var val = msg[1];
			play.mixer['karl'].inserts['karlTrigger'].set(\noveltyThresh,val);
		},'/noveltyThreshKarl');

		OSCdef(\karlOnsetThresh,{ |msg|
			var val = msg[1];
			play.mixer['karl'].inserts['karlTrigger'].set(\onsetThresh,val);
		},'/onsetThreshKarl');


		OSCdef(\janCalibrate,{ |msg|
			var onsets = msg[3];
			var novelty = msg[4];

			Routine{
				MKQTv2.guiAddr.sendBundle(0.001,
					["/noveltyJan",novelty * 0.5],
					["/onsetsJan",onsets * 0.5],
				);
				0.1.wait;
				MKQTv2.guiAddr.sendBundle(0.001,
					["/noveltyJan",0],
					["/onsetsJan",0],
				);
			}.play

		},'/janTrigger');


		OSCdef(\floCalibrate,{ |msg|
			var onsets = msg[3];
			var novelty = msg[4];

			Routine{
				MKQTv2.guiAddr.sendBundle(0.001,
					["/noveltyFlo",novelty * 0.5],
					["/onsetsFlo",onsets * 0.5],
				);
				0.1.wait;
				MKQTv2.guiAddr.sendBundle(0.001,
					["/noveltyFlo",0],
					["/onsetsFlo",0],
				);
			}.play

		},'/floTrigger');

		OSCdef(\karlCalibrate,{ |msg|
			var onsets = msg[3];
			var novelty = msg[4];

			Routine{
				MKQTv2.guiAddr.sendBundle(0.001,
					["/noveltyKarl",novelty * 0.5],
					["/onsetsKarl",onsets * 0.5],
				);
				0.1.wait;
				MKQTv2.guiAddr.sendBundle(0.001,
					["/noveltyKarl",0],
					["/onsetsKarl",0],
				);
			}.play

		},'/karlTrigger');


		/* ======= faders/mixer ======= */

		OSCdef(\janAmp,{ |msg|
			var val = msg[1];
			val = \db.asSpec.map(val).dbamp;
			play.mixer['jan'].amp = val;
		},'/janAmp');

		OSCdef(\floAmp,{ |msg|
			var val = msg[1];
			val = \db.asSpec.map(val).dbamp;
			play.mixer['flo'].amp = val;
		},'/floAmp');

		OSCdef(\karlAmp,{ |msg|
			var val = msg[1];
			val = \db.asSpec.map(val).dbamp;
			play.mixer['karl'].amp = val;
		},'/karlAmp');

		OSCdef(\naomiAmp,{ |msg|
			var val = msg[1];
			val = \db.asSpec.map(val).dbamp;
			play.mixer.sends['janFX'].amp = val;
			play.mixer.sends['floFX'].amp = val;
			play.mixer.sends['karlFX'].amp = val;
		},'/naomiAmp');

		OSCdef(\naomiActivity,{ |msg|
			var val = msg[1];
			val = val * 0.2;
			play.coinProb = val
		},'/naomiActivity');

		OSCdef(\mainAmp,{ |msg|
			var val = msg[1];
			val = \db.asSpec.map(val).dbamp;
			play.mixer.amp = val
		},'/mainAmp');

		/* ======= masterBus FX ======= */
		OSCdef(\eqXY,{ |msg|
			var freqs = msg[1,3..];
			var gains = msg[2,4..];
			play.eqBusses['freq'].setn(freqs);
			play.eqBusses['db'].setn(gains);

		},'/eqXY');

		OSCdef(\q,{ |msg|
			var val = msg[1].asString.interpret;
			play.eqBusses['rq'].setn(val)
		},'/q');

		OSCdef(\compAtk,{ |msg|
			var val = msg[1].asString.interpret;
			play.compBusses['atk'].setn(val)

		},'/compAtk');

		OSCdef(\compRls,{ |msg|
			var val = msg[1].asString.interpret;
			play.compBusses['rls'].setn(val)

		},'/compRls');

		OSCdef(\compRatio,{ |msg|
			var val = msg[1].asString.interpret;
			val = val.linlin(0,1,1,10);
			play.compBusses['ratio'].setn(val)

		},'/compRatio');

		OSCdef(\compKnee,{ |msg|
			var val = msg[1].asString.interpret;
			val = val.linlin(0,1,0,0.1);
			play.compBusses['knee'].setn(val)

		},'/compKnee');

		OSCdef(\compThresh,{ |msg|
			var val = msg[1..];
			val = val.collect({ |i| \db.asSpec.map(i) });
			play.compBusses['thresh'].setn(val)

		},'/compThresh');

		OSCdef(\compCrossO,{ |msg|
			var val = msg[1..];
			var logscale = (20480-20) / 10;
			val = val.collect({ |i| ((i * logscale.log).exp + 1) / logscale });
			val = val * 20480;

			play.compCrossO.setn(val)

		},'/compCrossO');

		OSCdef(\compMuGain,{ |msg|
			var val = msg[1..];
			val = val.collect({ |i| (i + 1).pow(2).log10 * 20 });
			play.compBusses['muGain'].setn(val)

		},'/compMuGain');

		/* ======= visuals ======= */

		OSCdef(\enterIP,{ |msg|
			var val = msg[1].asString;
			play.visualIP = val;
			"IP: %".format(val).postln;
		},'/enterIP');

		OSCdef(\enterPort,{ |msg|
			var val = msg[1].asInteger;
			play.visualPort = val;
			"port: %".format(val).postln;
		},'/enterPort');

		OSCdef(\visuals,{ |msg|
			var val = msg[1];
			play.visBool = val.asBoolean;
		},'/visuals');

		/* ======= play ======= */

		OSCdef(\startNaomi,{ |msg|
			var val = msg[1];
			if(val == 1,{ play.startPerformance },{ play.stopPerformance })
		},'/startNaomi');


	}
}