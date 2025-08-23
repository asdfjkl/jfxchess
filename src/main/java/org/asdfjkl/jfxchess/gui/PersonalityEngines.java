package org.asdfjkl.jfxchess.gui;

import java.util.ArrayList;
import org.asdfjkl.jfxchess.gui.Engine;
import org.asdfjkl.jfxchess.gui.EngineOption;

public class PersonalityEngines {

    private static EngineOption makeSpin(String name, int value) {
        EngineOption opt = new EngineOption();
        opt.type = EngineOption.EN_OPT_TYPE_SPIN;
        opt.name = name;
        opt.spinMin = -200;    // generic bounds, adjust if you know exact engine ranges
        opt.spinMax = 200;
        opt.spinDefault = value;
        opt.spinValue = value;
        return opt;
    }

    public static ArrayList<Engine> createEngines() {
        ArrayList<Engine> engines = new ArrayList<>();

        // 1. Benny the Beginner
        Engine benny = new Engine();
        benny.setName("Benny the Beginner");
        benny.addEngineOption(makeSpin("Contempt Factor", -20));
        benny.addEngineOption(makeSpin("Mobility (Midgame)", 60));
        benny.addEngineOption(makeSpin("Mobility (Endgame)", 60));
        benny.addEngineOption(makeSpin("Pawn Structure (Midgame)", 50));
        benny.addEngineOption(makeSpin("Pawn Structure (Endgame)", 50));
        benny.addEngineOption(makeSpin("Passed Pawns (Midgame)", 40));
        benny.addEngineOption(makeSpin("Passed Pawns (Endgame)", 40));
        benny.addEngineOption(makeSpin("Space", 60));
        benny.addEngineOption(makeSpin("Aggressiveness", 40));
        benny.addEngineOption(makeSpin("Cowardice", 140));
        benny.addEngineOption(makeSpin("Skill Level", 1));
        engines.add(benny);

        // 2. Lila the Learner
        Engine lila = new Engine();
        lila.setName("Lila the Learner");
        lila.addEngineOption(makeSpin("Contempt Factor", -10));
        lila.addEngineOption(makeSpin("Mobility (Midgame)", 80));
        lila.addEngineOption(makeSpin("Mobility (Endgame)", 70));
        lila.addEngineOption(makeSpin("Pawn Structure (Midgame)", 70));
        lila.addEngineOption(makeSpin("Pawn Structure (Endgame)", 60));
        lila.addEngineOption(makeSpin("Passed Pawns (Midgame)", 60));
        lila.addEngineOption(makeSpin("Passed Pawns (Endgame)", 50));
        lila.addEngineOption(makeSpin("Space", 80));
        lila.addEngineOption(makeSpin("Aggressiveness", 70));
        lila.addEngineOption(makeSpin("Cowardice", 120));
        lila.addEngineOption(makeSpin("Skill Level", 2));
        engines.add(lila);

        // 3. Captain Castle
        Engine castle = new Engine();
        castle.setName("Captain Castle");
        castle.addEngineOption(makeSpin("Contempt Factor", 0));
        castle.addEngineOption(makeSpin("Mobility (Midgame)", 70));
        castle.addEngineOption(makeSpin("Mobility (Endgame)", 70));
        castle.addEngineOption(makeSpin("Pawn Structure (Midgame)", 120));
        castle.addEngineOption(makeSpin("Pawn Structure (Endgame)", 130));
        castle.addEngineOption(makeSpin("Passed Pawns (Midgame)", 60));
        castle.addEngineOption(makeSpin("Passed Pawns (Endgame)", 80));
        castle.addEngineOption(makeSpin("Space", 60));
        castle.addEngineOption(makeSpin("Aggressiveness", 60));
        castle.addEngineOption(makeSpin("Cowardice", 130));
        castle.addEngineOption(makeSpin("Skill Level", 3));
        engines.add(castle);

        // 4. Zara the Zippy
        Engine zara = new Engine();
        zara.setName("Zara the Zippy");
        zara.addEngineOption(makeSpin("Contempt Factor", 10));
        zara.addEngineOption(makeSpin("Mobility (Midgame)", 140));
        zara.addEngineOption(makeSpin("Mobility (Endgame)", 130));
        zara.addEngineOption(makeSpin("Pawn Structure (Midgame)", 70));
        zara.addEngineOption(makeSpin("Pawn Structure (Endgame)", 60));
        zara.addEngineOption(makeSpin("Passed Pawns (Midgame)", 110));
        zara.addEngineOption(makeSpin("Passed Pawns (Endgame)", 90));
        zara.addEngineOption(makeSpin("Space", 130));
        zara.addEngineOption(makeSpin("Aggressiveness", 160));
        zara.addEngineOption(makeSpin("Cowardice", 40));
        zara.addEngineOption(makeSpin("Skill Level", 4));
        engines.add(zara);

        // 5. Gregory the Grinder
        Engine gregory = new Engine();
        gregory.setName("Gregory the Grinder");
        gregory.addEngineOption(makeSpin("Contempt Factor", 0));
        gregory.addEngineOption(makeSpin("Mobility (Midgame)", 90));
        gregory.addEngineOption(makeSpin("Mobility (Endgame)", 100));
        gregory.addEngineOption(makeSpin("Pawn Structure (Midgame)", 140));
        gregory.addEngineOption(makeSpin("Pawn Structure (Endgame)", 160));
        gregory.addEngineOption(makeSpin("Passed Pawns (Midgame)", 100));
        gregory.addEngineOption(makeSpin("Passed Pawns (Endgame)", 150));
        gregory.addEngineOption(makeSpin("Space", 90));
        gregory.addEngineOption(makeSpin("Aggressiveness", 80));
        gregory.addEngineOption(makeSpin("Cowardice", 90));
        gregory.addEngineOption(makeSpin("Skill Level", 5));
        engines.add(gregory);

        // 6. Mira the Magician
        Engine mira = new Engine();
        mira.setName("Mira the Magician");
        mira.addEngineOption(makeSpin("Contempt Factor", 5));
        mira.addEngineOption(makeSpin("Mobility (Midgame)", 130));
        mira.addEngineOption(makeSpin("Mobility (Endgame)", 120));
        mira.addEngineOption(makeSpin("Pawn Structure (Midgame)", 90));
        mira.addEngineOption(makeSpin("Pawn Structure (Endgame)", 90));
        mira.addEngineOption(makeSpin("Passed Pawns (Midgame)", 130));
        mira.addEngineOption(makeSpin("Passed Pawns (Endgame)", 130));
        mira.addEngineOption(makeSpin("Space", 120));
        mira.addEngineOption(makeSpin("Aggressiveness", 150));
        mira.addEngineOption(makeSpin("Cowardice", 50));
        mira.addEngineOption(makeSpin("Skill Level", 6));
        engines.add(mira);

        // 7. Sylvia the Strategist
        Engine sylvia = new Engine();
        sylvia.setName("Sylvia the Strategist");
        sylvia.addEngineOption(makeSpin("Contempt Factor", 10));
        sylvia.addEngineOption(makeSpin("Mobility (Midgame)", 120));
        sylvia.addEngineOption(makeSpin("Mobility (Endgame)", 110));
        sylvia.addEngineOption(makeSpin("Pawn Structure (Midgame)", 130));
        sylvia.addEngineOption(makeSpin("Pawn Structure (Endgame)", 140));
        sylvia.addEngineOption(makeSpin("Passed Pawns (Midgame)", 120));
        sylvia.addEngineOption(makeSpin("Passed Pawns (Endgame)", 130));
        sylvia.addEngineOption(makeSpin("Space", 110));
        sylvia.addEngineOption(makeSpin("Aggressiveness", 110));
        sylvia.addEngineOption(makeSpin("Cowardice", 70));
        sylvia.addEngineOption(makeSpin("Skill Level", 7));
        engines.add(sylvia);

        // 8. Victor the Viking
        Engine victor = new Engine();
        victor.setName("Victor the Viking");
        victor.addEngineOption(makeSpin("Contempt Factor", 20));
        victor.addEngineOption(makeSpin("Mobility (Midgame)", 150));
        victor.addEngineOption(makeSpin("Mobility (Endgame)", 130));
        victor.addEngineOption(makeSpin("Pawn Structure (Midgame)", 90));
        victor.addEngineOption(makeSpin("Pawn Structure (Endgame)", 80));
        victor.addEngineOption(makeSpin("Passed Pawns (Midgame)", 140));
        victor.addEngineOption(makeSpin("Passed Pawns (Endgame)", 120));
        victor.addEngineOption(makeSpin("Space", 150));
        victor.addEngineOption(makeSpin("Aggressiveness", 180));
        victor.addEngineOption(makeSpin("Cowardice", 30));
        victor.addEngineOption(makeSpin("Skill Level", 8));
        engines.add(victor);

        // 9. Helena the Huntress
        Engine helena = new Engine();
        helena.setName("Helena the Huntress");
        helena.addEngineOption(makeSpin("Contempt Factor", 25));
        helena.addEngineOption(makeSpin("Mobility (Midgame)", 140));
        helena.addEngineOption(makeSpin("Mobility (Endgame)", 140));
        helena.addEngineOption(makeSpin("Pawn Structure (Midgame)", 120));
        helena.addEngineOption(makeSpin("Pawn Structure (Endgame)", 130));
        helena.addEngineOption(makeSpin("Passed Pawns (Midgame)", 150));
        helena.addEngineOption(makeSpin("Passed Pawns (Endgame)", 160));
        helena.addEngineOption(makeSpin("Space", 130));
        helena.addEngineOption(makeSpin("Aggressiveness", 140));
        helena.addEngineOption(makeSpin("Cowardice", 20));
        helena.addEngineOption(makeSpin("Skill Level", 9));
        engines.add(helena);

        // 10. Chess Sorcerer
        Engine sorcerer = new Engine();
        sorcerer.setName("The Chess Sorcerer");
        sorcerer.addEngineOption(makeSpin("Contempt Factor", 30));
        sorcerer.addEngineOption(makeSpin("Mobility (Midgame)", 160));
        sorcerer.addEngineOption(makeSpin("Mobility (Endgame)", 150));
        sorcerer.addEngineOption(makeSpin("Pawn Structure (Midgame)", 150));
        sorcerer.addEngineOption(makeSpin("Pawn Structure (Endgame)", 160));
        sorcerer.addEngineOption(makeSpin("Passed Pawns (Midgame)", 160));
        sorcerer.addEngineOption(makeSpin("Passed Pawns (Endgame)", 180));
        sorcerer.addEngineOption(makeSpin("Space", 140));
        sorcerer.addEngineOption(makeSpin("Aggressiveness", 150));
        sorcerer.addEngineOption(makeSpin("Cowardice", 10));
        sorcerer.addEngineOption(makeSpin("Skill Level", 10));
        engines.add(sorcerer);

        return engines;
    }
}
