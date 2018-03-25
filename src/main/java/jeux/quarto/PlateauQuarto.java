package jeux.quarto;

import iia.jeux.modele.*;
import iia.jeux.modele.joueur.Joueur;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.lang.IllegalArgumentException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PlateauQuarto implements PlateauJeu{
    /********** commentaires *********/ 
    /// str Mouvement 

    
    /// Lignes = chiffres
    /// Colonne = lettres
    ///0x0000 4
    ///  0000 3
    ///  0000 2
    ///  0000 1
    ///  DCBA
    /// En bas à droite, on a la case A1 
    
    /// Opérateurs logiques sur les entiers
    ///    ^ -> XOR
    ///    | -> OR
    ///    & -> AND

    /// << -> Shift G
    /// >>> -> shift D logique (non signé)
    /// !! NE PAS UTILISER '>>' !! ///
    /// Il faut caster le résultat en byte/short pour les opérateurs au dessus
    
    /// Apparemment, pas d'entiers non-signés en java.
    // Don = 0, dépôt = 1
    // Syntaxe des pièces ("en commençant par les bits de poids fort="
    // b/r -> bleu , rouge. resp. 1 et 0 en notation binaire.
    // g/p -> grand, petit. resp. 1 et 0
    // p/t -> plein, troué. Resp. 1 et 0
    // c/r -> carré, rond . Resp. 1 et 0
    
    // Le joueur noir commence à donner une pièce
    
    // Note : il faut définir ce qui est ligne et ce qui est colonne

    /*******************
     *	
     * Attributs 
     *
     *******************/	
    
    // au lieu de faire j1 et j2, on appelle les joueurs j0 et j1 : J1 devient J0, j2 devient j1
    public static Joueur j0; 
    public static Joueur j1; 
    
    // Plateau de jeu
    private long plateau = 0;

    // Montre quelles pièces sont jouées.
    private short indCases = 0; 
    private short indPiece = 0; // On marque la pièce comme jouée lors du don de la pièce
    
    // id_joueur id_action 00 id_piexe
    private byte tourEtPiece = 0;


    /*************  constructeurs  ****************/

    /** 
     Constructeur de base de la classe
    **/
    public PlateauQuarto(){
    }

    /** 
     * Constructeur de PlateauQuarto initialisant le joueurs
     * @param joueurZero le premier joueur à jouer (qui va donner une pièce en premier
     * @param joueurUn le second joueur à jouer
     **/
    public PlateauQuarto(Joueur joueurZero, Joueur joueurUn){
	j0 = joueurZero;
	j1 = joueurUn;
    }
    
    /**
     * Constructeur initialisant à partir d'un état du plateau, 
     * 
     * @param plateau l'état du plateau à partir duquel jouer
     * @param indcases short dénotant des cases libres
     * @param indPiece short dénotant des cases jouées
     * @param tourEtPiece dénotant de l'état du tour
     **/
    private PlateauQuarto(long plateau, short indcases, short indpiece, byte tourEtPiece){
	this.plateau = plateau;
	this.indCases = indcases;
	this.indPiece = indpiece;
	this.tourEtPiece = tourEtPiece;
	// Pas besoin d'initialiser j1 et j2 puisque ce sont des attributs statiques 
    }
    
    /************ Méthodes privées ****************/

    /**
     * Méthode permettant d'accéder à l'identifiant de la pièce présente aux coordonnées (colonnes, lignes).
     * @param colonne la coordonnée "colonne" de la pièce à laquelle on veut accéder (entre 0 et 3 inclus)
     * @param ligne la coordonnée "ligne" de la pièce à laquelle on veut accéder (entre 0 et 3 inclus)
     * @return l'identifiant de la pièce accedée
     * @throws une Exception si la pièce n'est pas présente sur le plateau.
     * @see get_double_piece
     * @see points_communs
     ***/
    private byte get_piece (byte colonne, byte ligne) throws Exception {
	if ( (indCases >>> (ligne * 4 + colonne) ) % 2 == 0 )
	    throw new Exception();
       
	// Ligne et colonne sont compris entre 0 et 3
	return 
	    (byte) ((plateau >>> ((ligne*4 + colonne)*4)) & (0x0F));
    }

    /**
     * Méthode permettant d'accéder à l'identifiant de deux pièces côte à côte dans le plateau.
     * @param colonne la coordonnée "colonne" de la pièce de gauche à laquelle on veut accéder (entre 0 et 2 inclus)
     * @param ligne la coordonnée "ligne" des pièces auxquelles on veut accéder (entre 0 et 3 inclus)
     * @return l'identifiant des pièces accedée, sous la forme[id_piece1, id_piece2] 
     * @throws une Exception si une des pièces n'est pas présente sur le plateau.
     * @see get_piece
     * @see points_communs_double_pieces
     **/
    public byte get_double_piece(byte colonne, byte ligne) throws Exception {
	if ( (indCases >>> (ligne * 4 + colonne) ) % 2 == 0  || ( indCases >>> (ligne * 4 + colonne + 1) ) % 2 == 0 )
	    throw new Exception();
	
	// Ligne et colonne sont comris entre 0 et 3
	return (byte) (plateau >>> ((ligne*4 + colonne)*4));
    }

    /**
    * Prend deux identifiants de pièce en paramètre, rend les points communs de ces pièces
    * @param p1 l'identifiant de la première pièce, sous la forme 0x0[id_pièce].
    * @param p2 l'identifiant de la seconde pièce, même forme que p1
    * @return les points commus de ces deux pièces, de manière à ce que chaque bit représentant une caractéristique de la pièce soit à 1 si les identifiants ont cette caractéristique en commun
    * @see get_piece
    * @see points_communs_double_piece
    * @see test_colonne
    * @see test_diagonale
    ***/
    private byte points_communs(byte p1, byte p2){
	return (byte) (0x0F & ( ~ (p1 ^ p2) ));
    }

    
    /**
     * Fonction testant les points communs entre quatre pièces, dont les identi
     * @param db1 byte contenant les identifiants de deux pièces.
     * @param db2 byte contenant les identifiants de deux pièces.
     * @return un byte de la forme 0x0[points communs], ou chaque bit de caractéristique liée à un point commun est à 1 si cette caractéristique est commune à toutes les pièces
     * @see points_communs
     * @see test_ligne
     * @see test_carre
     ***/
    private byte points_communs_double_pieces(byte db1, byte db2){
	byte a = (byte) db1 >>> 4,
	    b = (byte) 0x0F & db1,
	    c = (byte) db2 >>> 4,
	    d = (byte) 0x0F & db2;
	return (byte) 0x0F & ((~ (a ^ b)) & (~ (a ^ c) ) & (~ (a ^ d) ));
    }
    
    /// id_colonne id_ligne < 3
    /**
     * Méthode testant si un carré contient quatre pièces du plateau ont une caractéristique en commun
     * @param id_colonne l'identifiant de la colonne du point le plus en haut à gauche du carré à tester (vers A1). Strictement inférieur à 3
     * @param id_ligne l'identifiant de la ligne du point le plus en haut à gauche du carré à tester (vers A1). Strictement inférieur à 3 
     * @return true si toutes les pièces du carré possèdent une caractéristique commun, false si elles n'en ont aucune (ou ne sont pas toutes posées)
     * @see test_ligne
     * @see test_diagonales
     * @see finDePartie
     ***/
    private boolean test_carre(byte id_colonne, byte id_ligne ){
	byte dp1, dp2;
	try {
	    dp1 = get_double_piece(id_colonne, id_ligne);
	    dp2 = get_double_piece(id_colonne, (byte) (id_ligne + 1));
	} catch( Exception e) {
	    return false;
	}
	
	return (points_communs_double_pieces(dp1, dp2) != 0x00);
    }

    /**
    * Méthode testant si les pièces d'une ligne possèdent un attribut en commun
    * @param id_ligne la ligne à tester
    * @return true si toutes les pièces posées sur la ligne en question possèdent un point commun, false si aucun point commun ou toutes les pièces ne sont pas posées
    * @see test_carre
    * @see test_diagonales
    * @see finDePartie
    * @see test_colonne
    ***/
    private boolean test_ligne(byte id_ligne){
	byte b1, b2;
	try {
	    b1 = get_double_piece((byte) 0x00, id_ligne);
	    b2 = get_double_piece((byte) 0x02, id_ligne);
	} catch (Exception e) {
	    return false;
	}
	
	return (points_communs_double_pieces(b1, b2) != 0x00);
    }
    
    /**
     * Méthode testant si les pièces des deux diagonales possèdent des attributs en commun, pour chaque diagonale 
    * @return true si toutes les pièces posées sur une des deux diagonales possèdent (au moins) un point commun, false si aucun point commun ou toutes les pièces ne sont pas posées
    * @see test_carre
    * @see test_lgine
    * @see finDePartie
    * @see test_colonne
    ***/
    private boolean test_diagonales(){
	byte [] g = new byte[4],
	        d = new byte[4];
	boolean g_false = false,
	    d_false = false;
	
	for(byte i = 0; i<4; i++){
	    try{
		g[i] =  get_piece(i, i);
	    } catch (Exception e) {
		g_false = true;
		if(d_false)
		    return false;
	    }

	    try{
			d[i] = get_piece( (byte) (0x03 - i), (byte) (0x03 - i));
	    } catch (Exception e) {
			d_false = true;
			if(g_false)
				return false;
	    }    
	}
	
	return true;
    }


    /**
    * Méthode testant si les pièces d'une colonne possèdent un attribut en commun
    * @param colonne la colonne à tester
    * @return true si toutes les pièces posées sur la colonne en question possèdent un point commun, false si aucun point commun ou toutes les pièces ne sont pas posées
    * @see test_carre
    * @see test_diagonales
    * @see finDePartie
    * @see test_ligne
    **/
    private boolean test_colonne(byte colonne){
	byte c1, c2, c3, c4;
	try{
	    c1 = get_piece(colonne, (byte) 0);
	    c2 = get_piece(colonne, (byte) 1);
	    c3 = get_piece(colonne, (byte) 2);
	    c4 = get_piece(colonne, (byte) 3);
	} catch (Exception e) {return false;}
	
	return (points_communs(c1, c2) & points_communs(c3, c4)) != 0;
    }
    
    /** Fonction déterminant si c'est le joueur 0 qui jouer 
     * @return true si le joueur 0 doit faire une action, false si c'est le j1
     **/
    private boolean j0plays(){
	return (tourEtPiece >>> 7) % 2  == 0;
    }
    
    /**
     * Fonction déterminant quel type de coup est joué
     * @return true si le coup qui doit être joué est un don
    **/
    private boolean is_don(){
	return (tourEtPiece >>> 6) % 2  == 0;
    }
    
    /**
     * Fonction jouant un dépôt de pièce
     * @param 
     *
     ***/
    private void unsafe_jouer_coup_depot(byte coup){
	// 1. Dépot de la pièce
	byte piece = (byte) (coup & 0x0F),
	    coord = (byte) (coup >>> 4);
	
	plateau = plateau | (piece << (4*coord));
	
	// 2. Marquer la coordonnée comme jouée 
	indCases = (short) (indCases | ((0x0001) << ((short) coord))); // -> on met un '1' au coord-ème bit (à partir de la droite) du short - qu'on présume à 0. 
	
	
	// 3. Changement du statut du tour : Le joueur venant de poser un pion
	//    donne une autre pièce à l'adversaire : On change le 2e bit
	//    de tourEtPiece.
	tourEtPiece = (byte) (0x40^tourEtPiece);
    }

    
    /* Aucun test. On part aussi du principe que piece est de la forme 0x0(pdpiece) */
    // ok    
    private void unsafe_jouer_coup_don(byte piece){
	// 1 : montrer la pièce qu'il faut jouer 
	tourEtPiece = (byte) (piece | (tourEtPiece & 0xF0));
	
	// 2 : Marquer la pièce comme jouée (même principe qu'au dessus)
	indPiece =(short) (indPiece | ((0x0001) << piece));
	
	// 3 : Changer l'état du tour. L'autre joueur va jouer un autre type de tour.
	tourEtPiece = (byte) (0xC0 ^ tourEtPiece);
	// 0xC = 0b1100
    }
    
    /********** Méthodes utiles pour les tests *********/

    /**
     * @brief renvoie la représentation sous un octet de la pièce mentionnée en paramètre
     * @param idpiece l'identifiant "string" de la pièce
     * @return l'identifiant de la pièce associée à la str associée en paramètre
     ***/
    public static byte stringToPiece(String strPiece){
	byte ret = 0x00;
	
	char [] idPiece = strPiece.toCharArray();
	
	// Pê il faudrait faire genre plutôt String.get(i)
	byte id_krq = 0x00;
	if( idPiece[0] == 'b' ) // b = bleu = blanc = 1
	    id_krq = 0x08; // 0b1000
	if( idPiece[1] == 'g' ) // g = grand = 1
	    id_krq = (byte) (0x04 ^ id_krq); // 0b0100
	if( idPiece[2] == 'p' ) // plèce pleine
	    id_krq = (byte) (0x02 ^ id_krq);
	if( idPiece[3] == 'c' ) // pièce carrée
	    id_krq = (byte) (0x01 ^ id_krq);
	
	return id_krq; 
    }

	// Erreur lors des tests mais je ne vois pas d'où pourrait venir l'erreur
	public boolean estchoixValide(String choose, String player) {
		for (byte i = 0; i < 4; i++) {
			for (byte j = 0; j < 4; j++) {
				byte ind = (byte) (i * 4 + j);

				if ((indCases >> ind) % 2 != 0) {
					byte id_piece = (byte) (0x0F & (plateau >>> (ind * 4)));
					String piece = pieceToString(id_piece);
					
					if(piece == choose)
						return false;
				}
			}
		}
		
		return true;
	}

    /**
     * @brief renvoie la représentation sous forme de chaine de caractères de la pièce en paramètre
     * @param idPiece l'identifiant de la pièce.
     * @return la chaine de caractères associée à l'identifiant
     ***/
    public static String pieceToString(byte idPiece){
	// Bleu/rouge, Grand/petit, Plein/troué, Rond/carré
	// Bleu = blanc, Rouge = noir
	
	char[] str = new char[4];
	
	if(idPiece % 2 ==  0) // 0 = rond
	    str[3] = 'r';
	else str[3] = 'c';
	
	if((idPiece >>> 1) % 2 == 0) // 0 = troué
	    str[2] = 't';
	else str[2] = 'p';
	
	if((idPiece>>> 2) % 2 == 0) // 0 = troué
	    str[1] = 'p';
	else str[1] = 'g';
	
	if((idPiece>>> 3) % 2 == 0) // 0 = troué
	    str[0] = 'r';
	else str[0] = 'b';

	
	return new String(str);
    }

    
    /********************************************************
     *
     * Méthodes de PlateauJeu 
     *
     ********************************************************/
    
    
    // Apparemment pas besoin de vérifier que c'est le bon joueur qui demande. On devrait pê faire une fonction genre "joueur jouant" ou quelque chose comme ça
    // ok    
    public ArrayList<CoupJeu> coupsPossibles(Joueur j) throws IllegalArgumentException {	
	ArrayList<CoupJeu> ret = new ArrayList<CoupJeu>();
	
	// Si c'est pas le bon joueur
	if (j0plays() && j.equals(j1) || (!j0plays() && j.equals(j0)))
	    throw new IllegalArgumentException("CoupsPossibles : Mauvais joueur demandé");
	
	if( is_don() ){ // Il faut donner la pièce
	    for(byte  i = 0; i<16; i++){
		if ( (indPiece >>> i) %2  == 0)
		    ret.add(new CoupQuarto( i ));
	    }
	    
	} else { // Sinon, dépôt de la pièce

	    for(byte  i = 0; i<16; i++){
		if ( (indCases >>> i) %2  == 0)
		    ret.add(new CoupQuarto( i ));
	    }
	}	
	return ret;
    }

    
    public void joue(Joueur j, CoupJeu cj) {
	
	// 1. vérification que c'est le bon joueur qui joue
	if( ! coupValide(j, cj) ) 
	    throw new IllegalArgumentException( "joue() : Coup invalide" );

	// On peut jouer le coup s'il est valide	
	CoupQuarto c = (CoupQuarto) cj;
	
	// 2. vérification du type du coup
	if( is_don() ){
	    byte idpiece = c.get();
	    unsafe_jouer_coup_don(idpiece);
	    
	} else { // C'est un dépôt
	    byte id_piece = c.get();
	    unsafe_jouer_coup_don( id_piece );
	}
    }
    
    
    public PlateauJeu copy() {
	return new PlateauQuarto(plateau, indCases, indPiece, tourEtPiece);
    }
    
    public boolean coupValide(Joueur j, CoupJeu cj) {
	CoupQuarto cq = (CoupQuarto) cj;
	byte id_coup = cq.get();
	
	return
	    // 1: vérification que c'est le bon joueur qui joue
	    ((j0plays() && j.equals(j0)) || (!j0plays() && j.equals(j1))) 
	    &&
	    
	    // 2 : Vérification de la validité du coup
	    (  is_don() && ((indPiece >>> id_coup) %2 == 0 )
	       || (!is_don()) && (indCases >>> id_coup) % 2 == 0) 	    ;
    }
   
    public boolean finDePartie(){
	for(byte i = 0; i<4; i++){
	/// Test des lignes
	    if(test_ligne(i) || test_colonne(i) ) return true;
	}
	/// Test des diagonales 
	if (test_diagonales()) return true;
	/// Test des carrés
	for(byte i = 0; i<3; i++)
	    for(byte j = 0; j<3; j++)
		if(test_carre(i, j)) return true;
		   

	/// Cas ou toutes les pièces ont été posées
	return indCases == 0xFFFF;
    }
    
    
    /*********** Méthodes de Partiel **************/
    
    /// TODO
    public static String coordToString(byte coordonnee_case){
	byte chiffre =(byte) (0x03 & coordonnee_case);
	byte lettre = (byte) ((0x0C & coordonnee_case) >>> 2);
	
	char char_lettre = '?';
	
	switch(lettre){
	case 0:
	    char_lettre = 'A';
	    break;
	case 1:
	    char_lettre = 'B';
	    break;
	case 2:
	    char_lettre = 'C';
	    break;
	case 3:
	    char_lettre = 'D';
	    break;
	default:
	    char_lettre = '?';
	    break;
	}
	
	return char_lettre + Integer.toString(chiffre);
    }
    
    // Modifié le 22/03
    // Note: Mouvement = dépot
    public boolean estmoveValide(String move, String player){
	CoupQuarto cj = new CoupQuarto(move);
	Joueur j;
	if (player.equals(str_j0) ) 
	    j = j0;
	else  j = j1;
	
	return coupValide(j, cj);
    }
    
    // impléemnté
    
    public String[] mouvementsPossibles(String player){
	Joueur j;
	if(player.equals(str_j0))
	    j = j0;
	else j = j1;
	ArrayList<CoupJeu> cj_arr;
	
	// Tester à quel moment du jeu on est, puis faire appel à 
	try {
	    cj_arr =  this.coupsPossibles(j) ;
	} catch ( IllegalArgumentException e ) {
	    return null;
	}
	
	String[] ret = new String[cj_arr.size()];
	boolean is_don = this.is_don();
	
	for(int i = 0; i<cj_arr.size(); i++){
	    CoupJeu cj = cj_arr.get(i);
	    CoupQuarto cq = ( CoupQuarto ) cj;
	    
	    ret[i] = cq.toString(is_don);
	}
	
	return ret;	
    }
    
    public String[] choixPossibles(String player){
	if(!is_don()) return null;

	return mouvementPossibles(player);
    }
       
    /// TODO: système d'exception (si possible internes) au lieu des return tous moisis    
    // player : noir = j0;
    // blanc = j1.
    /**
     * @brief joue le coup, matérialisé par une chaine de caractères.
     * Ce coup peut être sous deux formes différentes : deux lettres matérialisant une coordonnées (par 
     * ex. "A4", ou quatre lettres matérialisant une pièce ("rppc"), ou la première lettre représente la couleur, la deuxième représente 
     *  la taille, la troisième représente si la pièce est pleine ou vide, la quatrième représente
     * si ka mièce est carrée ou ronde.
     *
     * @param move le coup joué
     * @param player le joueur jouant le coup
     * @return rien si le coup est joué, sinon lance une exception 
     *
     * @version 
     **/
    /// Note : la vérification de la validité du coup se fait dans la méthode joue
    public void play(String move, String player){ 
	Joueur j;
	if( j0.toString().equals(player) )   
	    j = j0;
	else j = j1;
	CoupQuarto cq = new CoupQuarto(move);
	joue(j, cq);
    }
    
    /// Choose la pièce jouée
    /// move la case jouée
    /// Player le joueur jouant
    /// Si c'est censé être un don, aucun effet
    public void play(String choose, String move, String player){
	/// 1. vérifier qu'il faut bien poser une pièce. 
	if( is_don() ) return;

	/// 2. Vérifier que c'est la bonne pièce qui est jouée
	byte piece = strToPiece(choose);
	if( (tourEtPiece & 0x0F) != ((int) piece) ) return; // Comprendre : La pièce à jouer est différente de celle qu'on est censé jouer

	
	// 3. Jouer.
	play(move, player);
    }
    
    public Joueur getJ0() {
	return j0;
    }
    
    public Joueur getJ1() {
	return j1;
    }

    
    /*********** Méthodes de Partiel **************/
    
    // Autant séparer les tâches
    private void setFromStringTab(String[] s){
	// Note : on commence par les bits de poids faible.
	plateau  = 0;
	indCases = 0;
	indPieces= 0;
	tourEtPiece= 0;
	
	int ind_of_cases_seen = 0; // indiquera qu'on regardera 'telle' case de la ligne
	
	for(int i = 0; i<s.length; i++){
	    ind_of_cases_seen = 0;
	    String str = s[i];
	    for(int j = 0; j<str.length(); j++)
		if( str.charAt(j) == '+' ){ // Pas de pièce posée.
		    ind_of_cases_seen ++; 
		    continue;
		} else { // Sinon
		    String str_piece = str.substring(j, j+4); // Le second indice est exclusif
		    
		    j += 3; // Pour pas retomber sur la même chose. j+3 au lieu de j+4 étant donné que le j++ sera fait à la fin de la boucle
		    byte id_piece = strToPiece(str_piece);
		    
		    // on note la pièce comme jouée
		    indPieces = (short)  indPieces | ( 0x1 << id_piece );
		    
		    // Notation de la case comme jouée
		    short ind_case = i*4 + ind_of_cases_seen;
		     
		    indCases = (short)  indCases | (0x0001 << ind_case); 
		     
		    // Ajout de la pièce au plateau
		    plateau =  plateau | (0x1 << (ind_case*4));
		}
	}

	// à la fin, on regarde le nombre de pièces jouées pour déterminer le joueur qui doit jouer
	// Si ce nombre est pair, c'est J0 qui doit donner une pièce, sinon c'est J1.
	byte cpt = 0;
        for(byte i = 0; i<16; i++)
	    if((  indPieces >> i) % 2 == 1)
		cpt ++;
	
	if( cpt % 2 == 1 ) // Joueur = J1
	    tourEtPiece = (byte) 0x80;	
    }
    
    /// TODO
    public void setFromFile(String fileName) throws FileNotFoundException, IOException {
	// On peut calculer le joueur qui doit jouer en fonction du nombre de pièces posées.
	/// Note: étant donné qu'on utilise java préhistorique, ce code donne une erreur (car le bufferedReader doit pas être déclaré dans un try, je crois... Ou il faut un bloc "finally")
	
	try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
	    String line;
	    
	    int cpt_ligne = 0; // Compte le nombre de lignes intéressantes qu'on a déjà vu
	    
	    String tab_of_lignes = new String[4];
	    
	    while ( (line = br.readLine()) != null) {
		
		if (line.charAt(0) != '%') { // Si pas un commentaire
		    
		    String[] s = line.split(" "); // Séparation par les espaces
		    
		    /// De ce que je vois sur la fig3, la
		    /// Ligne ressemble à [CHIFFRE][ESPACE][ID DES PIECES/+][ESPACE][CHIFFRE].
		    /// Par conséquent on a juste besoin du truc au milieu
		    String ligne_plateau = s[1];
		    
		    tab_of_lignes[cpt_ligne] = ligne_plateau; // On garde la ligne intéressante
		    cpt_ligne ++;

		    // Si on a vu toutes les lignes intéressantes
		    if(cpt_ligne == 4)
			break;
		}
	    }
	    setFromStringTab( tab_of_lignes );
	}
    }
    

    /// TODO FIRST
    public String toString(){
	String ret = "";
	
	for(byte i = 0; i < 4; i++){
	    ret = ret + (i+1) + " ";

	    // si pas de pièce, ajout d'un +, si pièce ajout de l'id de la pièce
	    for(byte j = 0; j<4; j++){
		// id de la case qu'on regarde
		byte ind = i * 4 + j;
		
		// Si case inoccupée
		if((indCases >> ind) % 2 == 0) {
		    ret = ret + "+"; // Ajout d'un '+' dans la str, pour montrer qu'il n'y a pas de pièce
		} else { // Case occupée
		    // recherche de la pièce occupant la case
		    byte id_piece =  (byte) (0x0F & ( plateau >>> (ind * 4)));
		    ret = ret + pieceToString(id_piece);
		}
	    }
	    ret = ret + " " + (i+1) + "\n"; 
	}
	return ret
    }
    
    // Modifié le 22/03
    public void saveToFile(String fileName) throws Exception { 
	// TODO : Convertir le plateau en lignes de String -> on codera tout ça dans toString()
	// Note : il faudrait mettre les try
	PrintWriter output_shit = new Printwriter(filename);
	output_shit.write(this.toString());
	output_shit.close();
    }
}
