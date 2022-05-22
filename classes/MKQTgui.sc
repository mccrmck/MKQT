MKQTGUI {

	*startGUI {
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
						MKQTGUI.trainGUI

					}),
					Button()
					.string_("PLAY")
					.focusColor_(Color.clear)
					.font_( Font(fontString, 13) )
					.mouseUpAction_({ |but| but.states_([[ "PLAY",Color.black ]]) })
					.mouseDownAction_({ |but| but.states_([[ "PLAY",Color.red ]]) })
					.action_({ |but|
						win.close;
						MKQTGUI.playGUI
					}),
				),
			).spacing_(9)

		);

		win.onClose_({ "does the startUpwindow need a close function?".postln });
		// win.background_(Color.rand(0.5,1));
		win.drawFunc = {
			// fill the gradient
			Pen.addRect(win.view.bounds);
			Pen.fillAxialGradient(win.view.bounds.leftTop, win.view.bounds.rightBottom, Color.rand(0.3,1), Color.rand(0.3,1));
		};
		win.refresh;
		win.front;
	}

	*trainGUI {
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
						MKQTGUI.startGUI;

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
			Pen.fillAxialGradient(win.view.bounds.leftTop, win.view.bounds.rightBottom, Color.rand(0.3,1), Color.rand(0.3,1));
		};
		win.refresh;
		win.front;
	}

	*playGUI {}

}