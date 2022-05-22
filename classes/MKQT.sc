MKQT {
	classvar classifiers;

	*initClass {

		classifiers = IdentityDictionary(); // all classifiers get added to this dictionary and saved?

	}

	*new {
		^super.new.init
	}


	init {}

	*trainLive {
		var coefs = 13;

		Routine({
			SynthDef(\trainMKQTmfcc,{
				var sig = SoundIn.ar(\inBus.kr);
				var mfccs = FluidMFCC.kr(sig,coefs,40,1,maxNumCoeffs:coefs); // remove 0th coef
				FluidKrToBuf.kr(mfccs,\bufnum.kr);
				SendReply.kr(Impulse.kr(30),'/addPoints',mfccs);
			}).add;

		}).play
		// maxNumCoeffs must be declared when Synth is added - consider making a factory? Or setting a hi max? Or just using a fixed number?


	}

	*trainBuf {}

	*makeStartGui {
		var winW = 540, winH = 200;
		var center = Window.availableBounds.center;
		var bounds = Rect(center.x - (winW/2),center.y,winW,winH);
		var win = Window("MKQT",bounds);

		var fontString = "Kailasa";
		var bigFont = Font(fontString, 28);
		var titleFont = Font(fontString, 18);
		var subtitleFont = Font(fontString, 15);

		var inChanIndex = 0;
		var outChanIndex = 0;
		var sampleRate = 48000;

		win.layout_(

			VLayout(
				HLayout(
					StaticText().string_("Meat.Karaoke.Quality.Time").font_(bigFont).align_(\center),
				),
				StaticText().string_("1. SETUP").font_(titleFont),
				HLayout(
					VLayout(
						StaticText().string_("inDevice").font_(subtitleFont).align_(\center),
						PopUpMenu()
						.font_( Font(fontString,13) )
						.items_( ServerOptions.inDevices )
						.action_({ |menu|
							var inD = menu.item;
							Server.default.options.inDevice = inD;
							"new inDevice: %".format(inD).postln;
						}),
					),
					VLayout(
						StaticText().string_("outDevice").font_(subtitleFont).align_(\center),
						PopUpMenu()
						.font_( Font(fontString,13) )
						.items_( ServerOptions.outDevices )
						.action_({ |menu|
							var outD = menu.item;
							Server.default.options.outDevice = outD;
							"new outDevice: %".format(outD).postln;
						}),
					),
					VLayout(
						StaticText().string_("ins").font_(subtitleFont).align_(\center),
						PopUpMenu().items_( Array.fill(7,{|i| "% / %".format(i+1, i+2)}) )
						.font_( Font(fontString,13) )
						.action_({ |menu|
							var index = menu.value;
							inChanIndex = index;
						})
					),
					VLayout(
						StaticText().string_("outs").font_(subtitleFont).align_(\center),
						PopUpMenu().items_( Array.fill(7,{|i| "% / %".format(i+1, i+2)}) )
						.font_( Font(fontString,13) )
						.action_({ |menu|
							var index = menu.value;
							outChanIndex = index;
						})
					),
					VLayout(
						StaticText().string_("sampRate").font_(subtitleFont).align_(\center),
						PopUpMenu().items_( ["44100","48000","88200","96000"] )
						.font_( Font(fontString,13) )
						.value_(1)
						.action_({ |menu|
							var sRate = menu.item;
							sampleRate = sRate.asInteger;
							"new sampleRate: %".format(sRate).postln;
						})
					)
				),
				HLayout(
					StaticText().string_("2. PROCEED").font_(titleFont)
				),
				HLayout(
					Button()
					.string_("TRAIN")
					.focusColor_(Color.clear)
					.font_( Font(fontString, 13) )
					.mouseUpAction_({ |but| but.states_([["TRAIN",Color.black, ]]) })
					.mouseDownAction_({ |but| but.states_([["TRAIN",Color.red]]) })
					.action_({ |but|

						win.close;
						MKQT.makeTrainGui

					}),
					Button()
					.string_("PLAY")
					.focusColor_(Color.clear)
					.font_( Font(fontString, 13) )
					.mouseUpAction_({ |but| but.states_([[ "PLAY",Color.black ]]) })
					.mouseDownAction_({ |but| but.states_([[ "PLAY",Color.red ]]) })
					.action_({ |but|
						win.close;
						MKQT.makePlayGui
					}),
				),
			).spacing_(9)

		);

		win.onClose_({ "does the startUpwindow need a close function?".postln });
		// win.background_(Color.rand(0.5,1));
		win.drawFunc = {
			// fill the gradient
			Pen.addRect(win.view.bounds);
			Pen.fillAxialGradient(win.view.bounds.leftTop, win.view.bounds.rightBottom, Color.rand(0.5,1), Color.rand(0.5,1));
		};
		win.refresh;
		win.front;
	}

	*makeTrainGui {
		var winW = 700, winH = 200;
		var center = Window.availableBounds.center;
		var bounds = Rect(center.x - (winW/2),center.y,winW,winH);
		var win = Window("MKQT TRAIN",bounds);

		var fontString = "Kailasa";
		var bigFont = Font(fontString, 28);
		var titleFont = Font(fontString, 18);
		var subtitleFont = Font(fontString, 15);

		var stack, inputView, recordingView;

		inputView = View(win).layout_(
			HLayout(
				[ StaticText().string_("press play when ready, remember to press stop when done!").font_( Font(fontString,13) ),stretch: 0.5],
				Button()
				.focusColor_(Color.clear)
				.font_( Font(fontString, 13) )
				.states_( [[ "START",Color.black,Color.green(0.8)],["STOP",Color.black,Color.red(0.8)]] )
				.action_({

				})
			).spacing_(9).margins_(0)
		);

		recordingView = View(win).layout_(
			VLayout(
				StaticText().string_("drag in a sound file or folder:").font_( Font(fontString,13) ),
				HLayout(
					TextField()
					.action_({ |tField|
						tField.value.postln;

					}),
					Button()
					.focusColor_(Color.clear)
					.font_( Font(fontString, 13) )
					.states_( [[ "START",Color.black,Color.green(0.8)],["STOP",Color.black,Color.red(0.8)]] )
					.action_({

					}),
				),
			).spacing_(9).margins_(0)
		);

		win.layout_(

			VLayout(
				HLayout(
					StaticText().string_("Meat.Karaoke.Quality.Time").font_(bigFont).align_(\center)
				),

				HLayout([
					Button()
					.focusColor_(Color.clear)
					.font_( Font(fontString,13) )
					.states_([[ "back",Color.black,Color.clear ]])
					.mouseUpAction_({ |but| but.states_([[ "back",Color.black,Color.clear ]]) })
					.mouseDownAction_({ |but| but.states_([[ "back",Color.red,Color.clear ]]) })
					.action_({
						win.close;
						MKQT.makeStartGui;

					}), align: \left
				]),

				StaticText().string_("________").font_(subtitleFont).align_(\center),

				HLayout(
					[ StaticText().string_("1. CHOOSE MODE:").font_(subtitleFont).align_(\left), stretch: 0.1 ],
					PopUpMenu()
					.items_(["train on live input", "train on recording"])
					.action_({ |menu|
						stack.index = menu.value;
					}),
				),

				HLayout(
					stack = StackLayout(
						inputView,
						recordingView,
					)
				),

				StaticText().string_("________").font_(subtitleFont).align_(\center),

				HLayout(
					[ StaticText().string_("2. ENTER ONE-WORD CLASSIFIER:").font_(subtitleFont).align_(\left), stretch: 0.6],
					[ TextField()
						.font_(subtitleFont)
						.align_(\right)
						.action_({ |text|
							var string = text.string;
							"classifier: %".format(string).postln;
					}),stretch: 0.4]
				),

				StaticText().string_("________").font_(subtitleFont).align_(\center),

				HLayout(
					[ StaticText().string_("3. ADD TO DATASET:").font_(subtitleFont).align_(\left), stretch: 0.5],
					Button()
					.focusColor_(Color.clear)
					.font_( Font(fontString, 13) )
					.states_([[ "ADD",Color.black ]])
					.mouseUpAction_({ |but| but.states_([[ "ADD ",Color.black ]]) })
					.mouseDownAction_({ |but| but.states_([[ "ADD",Color.red ]]) })
					.action_({

					}),
				)
			).spacing_(9)
		);

		// win.background_(Color.rand(0.5,1));
		win.drawFunc = {
			// fill the gradient
			Pen.addRect(win.view.bounds);
			Pen.fillAxialGradient(win.view.bounds.leftTop, win.view.bounds.rightBottom, Color.rand(0.5,1), Color.rand(0.5,1));
		};
		win.refresh;
		win.front;
	}

	*makePlayGui {}


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

-when does "fitting" happen?? should it fit automatically or only on user request?
-automate fitting with a routine...can inform band that if they don't like how it's working they can 1. add more data 2. refit to get different results 3. Hire a new programmer??


PLAY
-check
-should have feedback button (map to spacebar?) that adds a point to dataset
-timed sections, change instruments/intensity/aesthetics

-MLPClassifier runs at .kr and tries to classify behaviour based on available choices... must have some filtering (sample and hold?) to ensure longer phrases and not wild switching
-switch statement a la EIDOLON makes decisions about what kind of processing happens.


*/
