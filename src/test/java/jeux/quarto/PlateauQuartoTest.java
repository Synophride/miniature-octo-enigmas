package jeux.quarto;

import iia.jeux.modele.joueur.Joueur;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PlateauQuartoTest {
    PlateauQuarto plateau_depart;
    PlateauQuarto plateau_final;
    // Syntaxe des pièces ("en commençant par les bits de poids fort="
    // b/r -> bleu , rouge. resp. 1 et 0 en notation binaire.
    // g/p -> grand, petit. resp. 1 et 0
    // p/t -> plein, troué. Resp. 1 et 0
    // c/r -> carré, rond . Resp. 1 et 0

    @Before
    public void init() {
        plateau_depart = new PlateauQuarto(new Joueur("blanc"), new Joueur("noir"));
	plateau_final = new PlateauQuarto(0xA8009F0000000000L , (short) 0xCC00, (short) 0x8700, (byte)0x00);
    }

    @Test
    public void testPlateauQuarto() {
        Assert.assertTrue(true);
    }
    
    @Test
    public void testCoupValide() {
	for(byte i = 0; i < 16; i++){
	    CoupQuarto c = new CoupQuarto(i, true);
	    Assert.assertTrue(plateau_depart.coupValide(plateau_depart.getJ0(), c));
	}
    }

    @Test
    public void testPieceToString() {
        Assert.assertEquals( PlateauQuarto.pieceToString((byte) 0x00) , "rptr");
        Assert.assertEquals( PlateauQuarto.pieceToString((byte) 0x01), "rptc");
	
        Assert.assertEquals( PlateauQuarto.pieceToString((byte) 0x08), "bptr");

	Assert.assertEquals( PlateauQuarto.pieceToString((byte) 0x0F), "bgpc");
    }

    @Test
    public void testEstMoveValide() {
        PlateauQuarto p = plateau_depart;
	
	// choix legit
        Assert.assertTrue(p.estchoixValide("bgpc", "noir") ); 
	
	// Mauvais type de coup
	Assert.assertFalse(p.estmoveValide("A1", "noir"));
	
	p.play("bgpc", "noir");

       
	// legit
        Assert.assertTrue(p.estmoveValide("A1", "blanc"));
	
	// Mauvais joueur
	Assert.assertFalse(p.estmoveValide("A1", "noir"));

	p.play("A1", "blanc");
	
	Assert.assertFalse(p.estchoixValide("rptr", "noir"));
	Assert.assertTrue( p.estchoixValide("rptr", "blanc"));
	Assert.assertFalse(p.estchoixValide("bgpc", "blanc"));
	
	// Mauvais type de coup
	Assert.assertFalse(p.estchoixValide("bgpc", "blanc"));
	p.play("rptr", "blanc");
    }
    
    @Test
    public void testEstChoixValide() {
        PlateauQuarto p = new PlateauQuarto();
	
        Assert.assertTrue(p.estchoixValide("bgpr", "noir"));
	
        p.play("bgpr", "noir");
	
	p.play("C3", "blanc");

	p.play("rgpr", "blanc");

	Assert.assertTrue(p.estchoixValide("A1", "noir"));
	//        p.play("A1", "noir");
	
	
        Assert.assertFalse( p.estchoixValide("bgpr", "blanc") );
    }
    
    @Test
    public void testFinDePartie() {
	Assert.assertFalse(plateau_depart.finDePartie());
	Assert.assertTrue(plateau_final.finDePartie());
        PlateauQuarto p = new PlateauQuarto(new Joueur("noir"), new Joueur("blanc"));
	
        String[] t = new String[4];
        t[0] = "bgprbgpcbgtcrgtc";
        t[1] = "++++";
        t[2] = "++++";
        t[3] = "++++";

        p.setFromStringTab(t);
        Assert.assertTrue(p.finDePartie());
    }


}
