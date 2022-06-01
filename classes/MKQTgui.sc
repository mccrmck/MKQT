MKQTGUI {

	// must fix all margins and spacing, as well as min/max width/height
	// make focusColor consistent
	// make fonts/font Sizes consistent
	// declare a bunch of font vars and shit up here!!

	classvar fontString = "Kailasa";

	*startGUI {
		var winW = 540, winH = 200;
		var center = Window.availableBounds.center;
		var bounds = Rect(center.x - (winW/2),center.y,winW,winH);
		var win = Window("M.K.Q.T.",bounds);

		var bigFont = Font(fontString, 28);
		var titleFont = Font(fontString, 18);
		var subtitleFont = Font(fontString, 15);
		var bgLeftGrad = Color.rand(0.3,1), bgRightGrad = Color.rand(0.3,1);

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

		win.onClose_({ "should the startUpwindow boot server onClose?".postln });
		// win.background_(Color.rand(0.5,1));
		win.drawFunc = {
			// fill the gradient
			Pen.addRect(win.view.bounds);
			Pen.fillAxialGradient(win.view.bounds.leftTop, win.view.bounds.rightBottom, bgLeftGrad, bgRightGrad);
		};
		win.refresh;
		win.front;
	}

	*trainGUI {
		var winW = 500, winH = 200;
		var center = Window.availableBounds.center;
		var bounds = Rect(center.x - (winW/2),center.y,winW,winH);
		var win = Window("M.K.Q.T. TRAIN",bounds);

		var bigFont = Font(fontString, 28);
		var titleFont = Font(fontString, 18);
		var subtitleFont = Font(fontString, 15);
		var bgLeftGrad = Color.rand(0.3,1), bgRightGrad = Color.rand(0.3,1);

		var stack, inputView, recordingView;
		var rViewText, rViewButton;

		inputView = View(win).layout_(
			HLayout(
				[ StaticText().string_("press play when ready, remember to press stop when done!").font_( Font(fontString,13) ),stretch: 0.5],
				Button()
				.focusColor_(Color.clear)
				.font_( Font(fontString, 13) )
				.states_( [[ "START",Color.black,Color.green(0.8)],["STOP",Color.black,Color.red(0.8)]] )
				.action_({

					"not implemented yet!!".warn

				})
			).spacing_(9).margins_(0)
		);

		rViewButton = Button()
		.focusColor_( Color.clear )
		.font_( Font(fontString, 13) )
		.states_( [[ "START",Color.black,Color.green(0.8)],["STOP",Color.black,Color.red(0.8)]] )
		.action_({
			rViewText.postln;
		});

		recordingView = View(win).layout_(
			VLayout(
				StaticText().string_("drag in a sound file or folder:").font_( Font(fontString,13) ),
				HLayout(
					TextField()
					.action_({ |tField|
						rViewText = tField.value;
					}),
					rViewButton,
				),
			).spacing_(4).margins_(0)
		);

		win.layout_(

			VLayout(
				HLayout(
					StaticText().string_("Meat.Karaoke.Quality.Time").font_(bigFont).align_(\center)
				),

				HLayout(
					[ Button()
						.focusColor_(Color.clear)
						.font_( Font(fontString,13) )
						.states_([[ "back",Color.black,Color.clear ]])
						.mouseUpAction_({ |but| but.states_([[ "back",Color.black,Color.clear ]]) })
						.mouseDownAction_({ |but| but.states_([[ "back",Color.red,Color.clear ]]) })
						.action_({
							win.close;
							MKQTGUI.startGUI;

					}), align: \left]
				).spacing_(0),

				HLayout( StaticText().string_("________").font_(subtitleFont).align_(\center) ),

				VLayout(
					HLayout(
						[ StaticText().string_("1. CHOOSE MODE:").font_(subtitleFont).align_(\left), stretch: 0.1 ],
						PopUpMenu()
						.items_(["train on recording","train on live input"])
						.action_({ |menu|
							stack.index = menu.value;
						}),
					),
					stack = StackLayout(
						recordingView,
						inputView,
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
					[ StaticText().string_("3. SAVE DATASET:").font_(subtitleFont).align_(\left), stretch: 0.5],
					Button()
					.focusColor_(Color.clear)
					.font_( Font(fontString, 13) )
					.states_([[ "SAVE",Color.black ]])
					.mouseUpAction_({ |but| but.states_([[ "SAVE",Color.black ]]) })
					.mouseDownAction_({ |but| but.states_([[ "SAVE",Color.red ]]) })
					.action_({

						"I think this should save a dataset \nand a labelset at the same time, right? \nAnd then they can be referenced together later?".postln

					}),
				)
			).spacing_(9)
		);

		// win.background_(Color.rand(0.5,1));
		win.drawFunc = {
			// fill the gradient
			Pen.addRect(win.view.bounds);
			Pen.fillAxialGradient(win.view.bounds.leftTop, win.view.bounds.rightBottom, bgLeftGrad, bgRightGrad);
		};
		// win.refresh;
		win.front;
	}

	*playGUI {
		var winW = 450, winH = 650;
		var center = Window.availableBounds.center;
		var bounds = Rect(center.x - (winW/2),center.y - (winH/2),winW,winH);
		var win = Window("M.K.Q.T. PLAY",bounds);

		var fontString = "Kailasa";
		var bigFont = Font(fontString, 28);
		var titleFont = Font(fontString, 18);
		var subtitleFont = Font(fontString, 15);
		var bgLeftGrad = Color.rand(0.3,1), bgRightGrad = Color.rand(0.3,1);

		var loadStack, newLoadView, oldLoadView = View(win);
		var playStack, oldPlayView, newPlayView;

		var ezSlider;

		var waitBeforeStart = 0;

		oldLoadView.layout_(
			HLayout(
				[ StaticText().string_("select a neural net").font_( Font(fontString,13) ) ],
				[ Button()
					.states_([["OPEN"]])
					.font_( Font(fontString,13) )
					.action_({
						var folderPath = Platform.userExtensionDir +/+ "MKQT/neuralNets/";
						FileDialog({|path|
							// check for file types? If .json.not, throw an error?

							path.unbubble.postln;
							oldLoadView.layout.add(
								StaticText()
								.font_( Font(fontString,13) )
								.string_( PathName(path.unbubble).fileNameWithoutExtension )
								.align_(\right)
							)

						},{},1,0,false,folderPath);
				}) ],
			).spacing_(9).margins_(0)
		);

		newLoadView = View(win).layout_(
			HLayout(
				StaticText().string_("select one or more datasets").font_( Font(fontString,13) ),
				Button()
				.states_([["OPEN"]])
				.font_( subtitleFont )
				.action_({
					var folderPath = Platform.userExtensionDir +/+ "MKQT/dataSets/";
					FileDialog({ |paths|

						MKQT.makeDataAndLabelSets(paths);

					},{},3,0,false,folderPath);
				}),
				Button()
				.states_( [[ "FIT"]] )
				.font_( subtitleFont )
				.action_({ |but|

				})

			).spacing_(9).margins_(0)
		);

		oldPlayView = View(win).layout_(
			HLayout(
				[ Button()
					.states_( [[ "PLAY",Color.black,Color.green(0.8)],["STOP",Color.black,Color.red(0.8)]] )
					.font_( subtitleFont )
					.action_({ |but|

						Routine({
							waitBeforeStart.wait;

						}).play

				}), align: \center]
			).spacing_(9).margins_(0),
		);

		newPlayView = View(win).layout_(
			HLayout(
				Button()
				.states_( [[ "PLAY",Color.black,Color.green(0.8)],["STOP",Color.black,Color.red(0.8)]] )
				.font_( subtitleFont )
				.action_({ |but|

					Routine({
						waitBeforeStart.wait;

					}).play

				}),
				Button()
				.states_( [[ "SAVE NEURAL NET",Color.black,Color.green(0.8)]] )
				.font_( subtitleFont )
				.action_({ |but|

				}),

			).spacing_(9).margins_(0),
		);

		ezSlider = { |string, specKey|
			var text = StaticText();
			var slider = Slider();
			var numBox = NumberBox();

			VLayout(
				text
				.string_(string)
				.font_( subtitleFont )
				.align_(\center),

				slider
				.action_({ |slider|
					var val = slider.value;
					numBox.value = specKey.asSpec.map(val).round(0.1);

				})
				.valueAction_(0),

				numBox
				.font_( subtitleFont )
				.align_(\center)
				.action_({|box|
					var val = box.value;
					slider.value = specKey.asSpec.unmap(val);
				})
			).margins_(9)
		};

		win.layout_(

			VLayout(
				HLayout(
					StaticText().string_("Meat.Karaoke.Quality.Time").font_(bigFont).align_(\center)
				),

				HLayout(
					[ Button()
						.focusColor_(Color.clear)
						.font_( Font(fontString,13) )
						.states_([[ "back",Color.black,Color.clear ]])
						.mouseUpAction_({ |but| but.states_([[ "back",Color.black,Color.clear ]]) })
						.mouseDownAction_({ |but| but.states_([[ "back",Color.red,Color.clear ]]) })
						.action_({
							win.close;
							MKQTGUI.startGUI;

					}), align: \left]

				).spacing_(0),

				HLayout(
					[ StaticText().string_("1. PERFORMANCE LENGTH:").font_(subtitleFont).align_(\left), stretch: 0.5],
					[ PopUpMenu()
						.items_(Array.fib(6,3,5))
						.value_(4)
						.action_({ |menu|

						}),
						align: \center],
					[ StaticText().string_("MINUTES").font_(subtitleFont).align_(\left)],
				),

				HLayout(
					[ StaticText().string_("2. SELECT MIDI DEVICE:").font_(subtitleFont).align_(\left) ],
					[ PopUpMenu()
						.items_( MIDIClient.destinations.collect({ |m| m.device.asString }) )
						.action_({ |menu|

					}) ],
				),

				HLayout(
					StaticText().string_("3. WAIT").font_(subtitleFont),
					NumberBox()
					.align_(\center)
					.value_(0)
					.action_({ |num|

						waitBeforeStart = num.value;
						num.value.postln;
					}).maxWidth_(80),
					StaticText().string_("SECONDS BEFORE STARTING").font_(subtitleFont).minWidth_(250).align_(\right),
				),

				VLayout(
					HLayout(
						[ StaticText().string_("4. PERFORMANCE MODE:").font_(subtitleFont).align_(\left), stretch: 0.5],
						[ PopUpMenu()
							.items_(["old neural net","new neural net" ])
							.action_({ |menu|
								loadStack.index = menu.value;
								playStack.index = menu.value;
							}),
							align: \right],
					),
					loadStack = StackLayout(
						oldLoadView,
						newLoadView
					),
					playStack = StackLayout(
						oldPlayView,
						newPlayView
					)
				).spacing_(9),

				HLayout(
					ezSlider.value("band dB",\db),
					ezSlider.value("PC dB",\db),
					ezSlider.value("PC activity",\unipolar),
					ezSlider.value("",\unipolar),
				)

			).spacing_(9)
		);

		// win.background_(Color.rand(0.5,1));
		win.drawFunc = {
			// fill the gradient
			Pen.addRect(win.view.bounds);
			Pen.fillAxialGradient(win.view.bounds.leftTop, win.view.bounds.rightBottom, bgLeftGrad, bgRightGrad);
		};
		win.refresh;
		win.front;
	}

}