/* JFXChess - A Chess Graphical User Interface
 * Copyright (C) 2020-2025 Dominik Klein
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.asdfjkl.jfxchess.gui;

import java.util.ArrayList;

public class BotEngines {

    private static EngineOption makeSpin(String name, int value) {
        EngineOption opt = new EngineOption();
        opt.type = EngineOption.EN_OPT_TYPE_SPIN;
        opt.name = name;
        opt.spinMin = 1;
        opt.spinMax = 200;
        opt.spinDefault = value;
        opt.spinValue = value;
        return opt;
    }

    public static ArrayList<BotEngine> createEngines(String path) {
        ArrayList<BotEngine> botEngines = new ArrayList<>();

        // bot engine elo roughly approximated as glicko2 by engine
        // matches against maia9, using maia9 lichess classical 1694

        // 1. Benny the Beginner
        BotEngine benny = new BotEngine();
        benny.setPath(path);
        benny.setName("Benny the Beginner");
        benny.setElo("1612");
        benny.setBio("Benny just learned how to move the pieces yesterday. He’s enthusiastic, " +
                "sometimes forgets about his king’s safety, but always has fun no matter the result.");
        benny.loadImage("bots/01_Benny_the_Beginner.png");
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
        botEngines.add(benny);

        // 2. Lila the Learner
        BotEngine lila = new BotEngine();
        lila.setPath(path);
        lila.setName("Lila the Learner");
        lila.setElo("1685");
        lila.setBio("Lila is bright and eager, with a growing collection of chess books she " +
                "doesn’t quite understand yet. She loves developing her pieces but gets easily " +
                "distracted by  “fun” moves.");
        lila.loadImage("bots/02_Lila_the_Learner.png");
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
        botEngines.add(lila);

        // 3. Captain Castle
        BotEngine castle = new BotEngine();
        castle.setPath(path);
        castle.setName("Captain Castle");
        castle.setElo("1840");
        castle.setBio("A cautious player who loves to tuck his king safely away before doing " +
                "anything else. His friends joke he plays chess “like building a fortress.”");
        castle.loadImage("bots/03_Captain_Castle.png");
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
        botEngines.add(castle);

        // 4. Zara the Zippy
        BotEngine zara = new BotEngine();
        zara.setPath(path);
        zara.setName("Zara the Zippy");
        zara.setElo("1912");
        zara.setBio("Quick-thinking and daring, Zara rushes into attacks before you’ve had time " +
                "to blink. She’s unpredictable and loves flashy sacrifices.");
        zara.loadImage("bots/04_Zara_the_Zippy.png");
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
        botEngines.add(zara);

        // 5. Gregory the Grinder
        BotEngine gregory = new BotEngine();
        gregory.setPath(path);
        gregory.setName("Gregory the Grinder");
        gregory.setElo("1994");
        gregory.setBio("Patient and methodical, Gregory wins by slowly squeezing his opponents. " +
                "He thrives in long endgames where his careful pawn moves shine.");
        gregory.loadImage("bots/05_Gregory_the_Grinder.png");
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
        botEngines.add(gregory);

        // 6. Mira the Magician
        BotEngine mira = new BotEngine();
        mira.setPath(path);
        mira.setName("Mira the Magician");
        mira.setElo("2095");
        mira.setBio("Mira dazzles opponents with unexpected tactical tricks. She seems to pull moves " +
                "out of thin air, turning lost positions into victories.");
        mira.loadImage("bots/06_Mira_the_Magician.png");
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
        botEngines.add(mira);

        // 7. Sylvia the Strategist
        BotEngine sylvia = new BotEngine();
        sylvia.setPath(path);
        sylvia.setName("Sylvia the Strategist");
        sylvia.setElo("2040");
        sylvia.setBio("Sylvia plans her games like military campaigns, always several moves ahead. " +
                "She’s positionally sound and rarely falls for traps.");
        sylvia.loadImage("bots/07_Sylvia_the_Strategist.png");
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
        botEngines.add(sylvia);

        // 8. Victor the Viking
        BotEngine victor = new BotEngine();
        victor.setPath(path);
        victor.setName("Victor the Viking");
        victor.setElo("2022");
        victor.setBio("Fearless and aggressive, Victor charges forward in attack like a raid " +
                "on the chessboard. He’s a terror in open positions.");
        victor.loadImage("bots/08_Victor_the_Viking.png");
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
        botEngines.add(victor);

        // 9. Helena the Huntress
        BotEngine helena = new BotEngine();
        helena.setPath(path);
        helena.setName("Helena the Huntress");
        helena.setElo("2086");
        helena.setBio("Helena stalks her opponent’s weaknesses with deadly precision, combining " +
                "tactics and strategy seamlessly. Her endgames are as sharp as her middlegame attacks.");
        helena.loadImage("bots/09_Helena_the_Huntress.png");
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
        botEngines.add(helena);

        // 10. Chess Sorcerer
        BotEngine sorcerer = new BotEngine();
        sorcerer.setPath(path);
        sorcerer.setName("The Chess Sorcerer");
        sorcerer.setElo("2131");
        sorcerer.setBio("The legendary Chess Sorcerer, whose wisdom spans centuries of play. Every " +
                "move feels inevitable, as if he knows the entire game before it starts.");
        sorcerer.loadImage("bots/10_The_Chess_Sorcerer.png");
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
        botEngines.add(sorcerer);

        return botEngines;
    }
}
