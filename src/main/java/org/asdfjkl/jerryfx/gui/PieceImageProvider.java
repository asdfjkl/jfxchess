package org.asdfjkl.jerryfx.gui;

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.SVGUniverse;
import javafx.scene.image.Image;
import org.asdfjkl.jerryfx.lib.CONSTANTS;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import javafx.embed.swing.SwingFXUtils;

import static org.asdfjkl.jerryfx.gui.BoardStyle.*;

public class PieceImageProvider {

    HashMap<Integer, Image> whitePawns = new HashMap<>();
    HashMap<Integer, Image> blackPawns = new HashMap<>();

    HashMap<Integer, Image> whiteRooks = new HashMap<>();
    HashMap<Integer, Image> blackRooks = new HashMap<>();

    HashMap<Integer, Image> whiteKnights = new HashMap<>();
    HashMap<Integer, Image> blackKnights = new HashMap<>();

    HashMap<Integer, Image> whiteBishops = new HashMap<>();
    HashMap<Integer, Image> blackBishops = new HashMap<>();

    HashMap<Integer, Image> whiteQueens = new HashMap<>();
    HashMap<Integer, Image> blackQueens = new HashMap<>();

    HashMap<Integer, Image> whiteKings = new HashMap<>();
    HashMap<Integer, Image> blackKings = new HashMap<>();

    private BufferedImage renderSVG(URL urlPieceSVG, int width, int height) {

        SVGUniverse svgUniverse = new SVGUniverse();
        SVGDiagram diagram = null;
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        try {
            diagram = svgUniverse.getDiagram(svgUniverse.loadSVG(urlPieceSVG));
            diagram.setIgnoringClipHeuristic(true);
            AffineTransform at = new AffineTransform();
            at.setToScale(width/diagram.getWidth(), height/diagram.getWidth());
            Graphics2D ig2 = bi.createGraphics();
            ig2.transform(at);
            ig2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            diagram.render(ig2);
        } catch (SVGException e) {
            e.printStackTrace();
        }
        return bi;
    }

    private HashMap<Integer, Image> getHashMap(int pieceType) {
        if(pieceType == CONSTANTS.WHITE_PAWN) {
            return whitePawns;
        }
        if(pieceType == CONSTANTS.BLACK_PAWN) {
            return blackPawns;
        }
        if(pieceType == CONSTANTS.WHITE_ROOK) {
            return whiteRooks;
        }
        if(pieceType == CONSTANTS.BLACK_ROOK) {
            return blackRooks;
        }
        if(pieceType == CONSTANTS.WHITE_KNIGHT) {
            return whiteKnights;
        }
        if(pieceType == CONSTANTS.BLACK_KNIGHT) {
            return blackKnights;
        }
        if(pieceType == CONSTANTS.WHITE_BISHOP) {
            return whiteBishops;
        }
        if(pieceType == CONSTANTS.BLACK_BISHOP) {
            return blackBishops;
        }
        if(pieceType == CONSTANTS.WHITE_QUEEN) {
            return whiteQueens;
        }
        if(pieceType == CONSTANTS.BLACK_QUEEN) {
            return blackQueens;
        }
        if(pieceType == CONSTANTS.WHITE_KING) {
            return whiteKings;
        }
        if(pieceType == CONSTANTS.BLACK_KING) {
            return blackKings;
        }
        throw  new IllegalArgumentException("called with, but there is no such piece: "+pieceType);
    }

    private String getFilename(int pieceType) {

            if(pieceType == CONSTANTS.WHITE_PAWN) {
                return "wp.svg";
            }
            if(pieceType == CONSTANTS.BLACK_PAWN) {
                return "bp.svg";
            }
            if(pieceType == CONSTANTS.WHITE_ROOK) {
                return "wr.svg";
            }
            if(pieceType == CONSTANTS.BLACK_ROOK) {
                return "br.svg";
            }
            if(pieceType == CONSTANTS.WHITE_KNIGHT) {
                return "wn.svg";
            }
            if(pieceType == CONSTANTS.BLACK_KNIGHT) {
                return "bn.svg";
            }
            if(pieceType == CONSTANTS.WHITE_BISHOP) {
                return "wb.svg";
            }
            if(pieceType == CONSTANTS.BLACK_BISHOP) {
                return "bb.svg";
            }
            if(pieceType == CONSTANTS.WHITE_QUEEN) {
                return "wq.svg";
            }
            if(pieceType == CONSTANTS.BLACK_QUEEN) {
                return "bq.svg";
            }
            if(pieceType == CONSTANTS.WHITE_KING) {
                return "wk.svg";
            }
            if(pieceType == CONSTANTS.BLACK_KING) {
                return "bk.svg";
            }
            throw  new IllegalArgumentException("called with, but there is no such piece: "+pieceType);
    }


    public Image getImage(int piece, int squareSize, int pieceStyle) {

        HashMap<Integer, Image> pieceHashMap = getHashMap(piece);
        String svgPieceName = getFilename(piece);

        if(pieceHashMap.containsKey(squareSize)) {
            return pieceHashMap.get(squareSize);
        } else {
            String fn = "pieces/";
            if(pieceStyle == PIECE_STYLE_MERIDA) {
                fn += "merida/";
            }
            if(pieceStyle == PIECE_STYLE_OLD) {
                fn += "old/";
            }
            if(pieceStyle == PIECE_STYLE_USCF) {
                fn += "uscf/";
            }
            fn += svgPieceName;
            URL urlSVG = getClass().getClassLoader().getResource(fn);
            BufferedImage bufferedImagePiece = renderSVG(urlSVG, squareSize, squareSize);
            Image img = SwingFXUtils.toFXImage(bufferedImagePiece, null);
            pieceHashMap.put(squareSize, img);
            return img;
        }
    }
}
