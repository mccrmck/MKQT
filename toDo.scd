






// check cpu use of FluidSpectralShape.kr() + FluidStats.kr in real time!!
// make synthLib classvar with keys from the synthLib - this can aid the process when adding new genre dataSets
// make OSCdef that assigns the genreState variable
// make OSCdefs that choose synths from lib based on genreState
// what happens at time boundaries? How does the "sound aesthetic" change?
// does the synthLib have args in each event as well? Or are they just arg indexes? Or how do I handle this smartly/algorithmically?
// synthKeys must have a similar logic like EIDOLON - long phrases of the same fx...maybe these get changed at the fibonacci barlines?

// synthLib needs a default key for when new datasets return a genre that doens't have synths yet!
// when data/Label set is created, it builds a synth Library also - anything that is nil gets filled with Default synths!




OSCdef(\needsName,{ |msg, time, addr, recvPort|
	msg.postln;
	index = msg[3];
	genreState = classifiers[index];
},'needs/path');

OSCdef(\needsKeys,{ |msg, time, addr, recvPort|
	msg.postln;
	var onsetTrig =  msg[3]; // ? is this right?
	var onsettrig =  msg[3]; // ? is this right?
	// var bus =  msg[5]; // ? is this right?



},'needs/path/too')


