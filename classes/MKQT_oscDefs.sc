+MKQT2{

	guiOSCdefs {
		var janIn, floIn, karlIn1, karlIn2, mainOut;

		OSCdef(\initGUI,{

			MKQT2.guiAddr.sendMsg("/inDeviceList", *ServerOptions.inDevices);
			MKQT2.guiAddr.sendMsg("/outDeviceList",*ServerOptions.outDevices)

		},'/guiLoaded');

		OSCdef(\inDevice,{ |msg|
			var val = msg[1];

			Server.default.options.inDevice = val;
			"Audio input device: %".format(val).postln;

		},'/inputDevice');

		OSCdef(\outDevice,{ |msg|
			var val = msg[1];

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

		OSCdef(\karlIn1,{ |msg|
			var val = msg[1].asString.split($/);
			val = val.collect({ |i| i.interpret - 1 });
			karlIn1 = val

		},'/karlIn1');

		OSCdef(\karlIn2,{ |msg|
			var val = msg[1].asString.split($/);
			val = val.collect({ |i| i.interpret - 1 });
			karlIn2 = val

		},'/karlIn2');

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
			MKQT2.trainOSCdefs(train);

		},'/train');

		OSCdef(\playButton,{ |msg|
			var jan = janIn ? 0;
			var flo = floIn ? 1;
			var karl1 = karlIn1 ?[4,5];
			var karl2 = karlIn2 ? [6,7];

			var play = MKQTPlay(jan, flo, karl1, karl2, mainOut);
			MKQT2.playOSCdefs(play)
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

		OSCdef(\fxAmp,{ |msg|
			var val = msg[1];
			val = \db.asSpec.map(val).dbamp;
			train.mixer['playback'].amp = val

		},'/effectsAmp');

		OSCdef(\pbAmp,{ |msg|
			var val = msg[1];
			val = \db.asSpec.map(val).dbamp;
			train.mixer['playback'].preSends['fx'].set(\amp,val)
		},'/playbackAmp');





		/* ======= faders/mixer ======= */

	}

	*playOSCdefs { |instance|
		var play = instance;
		var eqBusses = play.eqBusses;
		var compBusses = play.compBusses;



		/* ======= calibration ======= */

		OSCdef(\janCalibrate,{ |msg|
			msg.postln

		},'/janTrigger');

		OSCdef(\floCalibrate,{ |msg|
			msg.postln

		},'/floTrigger');

		OSCdef(\karlCalibrate,{ |msg|
			msg.postln

		},'/karlTrigger');



		/* ======= masterBus FX ======= */
		OSCdef(\eqXY,{ |msg|
			var freqs = msg[1,3..];
			var gains = msg[2,4..];
			eqBusses['freq'].setn(freqs);
			eqBusses['db'].setn(gains)
		},'/eqXY');

		OSCdef(\q,{ |msg|
			var val = msg[1].asString.interpret;
			eqBusses['rq'].setn(val)
		},'/q');

		OSCdef(\compAtk,{ |msg|
			var val = msg[1].asString.interpret;
			compBusses['atk'].setn(val)

		},'/compAtk');

		OSCdef(\compRls,{ |msg|
			var val = msg[1].asString.interpret;
			compBusses['rls'].setn(val)

		},'/compRls');

		OSCdef(\compRatio,{ |msg|
			var val = msg[1].asString.interpret;
			val = val.linlin(0,1,1,10);
			compBusses['ratio'].setn(val)

		},'/compRatio');

		OSCdef(\compKnee,{ |msg|
			var val = msg[1].asString.interpret;
			val = val.linlin(0,1,0,0.1);
			compBusses['knee'].setn(val)

		},'/compKnee');

		OSCdef(\compThresh,{ |msg|
			var val = msg[1..];
			val = val.collect({ |i| \db.asSpec.map(i) });
			compBusses['thresh'].setn(val)

		},'/compThresh');

		OSCdef(\compCrossO,{ |msg|
			var val = msg[1..];
			var logscale = (20480-20) / 10;
			val = val.collect({ |i| ((i * logscale.log).exp + 1) / logscale });
			val = val * 20480;

			this.compCrossO.setn(val)

		},'/compCrossO');

		OSCdef(\compMuGain,{ |msg|
			var val = msg[1..];
			val = val.collect({ |i| (i + 1).pow(2).log10 * 20 });
			compBusses['muGain'].setn(val)

		},'/compMuGain');



		/* ======= faders/mixer ======= */

		OSCdef(\mainAmp,{ |msg|
			var val = msg[1];
			val = \db.asSpec.map(val).dbamp;
			this.mixer.amp = val

		},'/pcAmp');

	}
}