Version 1.1.8 - 23/05/2015
- fix crash quand Radis est rappelé après un certain temps en background
- update des lib android support et optimisation du rafraichissement de la liste des opérations

Version 1.1.7 - 16/05/2015
- stabilisation
- fix affichage d'opération

Version 1.1.6 - 14/05/2015
- stabilisation
- fix affichage quand un compte n'a que des opération de 3 mois ou plus

Version 1.1.5 - 12/05/2015
- stabilisation
- fix de bug sur les statistics (dates incorrectes, suppression de stat quand le compte concerné est supprimé)

Version 1.1.4 - 02/05/2015
- stabilisation

Version 1.1.3 - 29/04/2015
- fix d'un autre crash

Version 1.1.2 - 28/04/2015
- tentative de fix de crash difficile (pas d'info précise sur comment ça arrive :( ) 

Version 1.1.1 - 27/04/2015
- fix de crash

Version 1.1 - 24/04/2015
- option pour configurer le nombre de mois à insérer
- option pour choisir l'action du tap long de l'ajout rapide
- chaque compte peut définir ses propres options
- fix de crash
- fix de bugs variés
- encore plus de Kotlin

Version 1.0 - 21/01/2015
- graphiques !
- navigation par panneau latéral
- fusion du pointage dans la liste des opérations
- compatibilité Android 5
- sous le capot, Kotlin (plus de détails sur http://geobert.fr/2015/01/13/radis-1-0)

Version 0.9.3 - 03/05/2014
- correction d'un crash
- correction de problème de projection après une insertion d'échéances (faites un "recalculer le compte" pour remettre le tout correctement, ça ne devrait plus se corrompre)
- nouveau : les propositions pour l'ajout rapide sont affichés en ordre inverse d'utilisation pour avoir les plus utilisés au plus proche du champs texte (désactivable dans les options)

Version 0.9.2 - 29/04/2014
- corrections de crashs
- correction de la logique sur les transferts : somme >= 0 depuis la 0.9, et dans ce cas, si A->B alors -somme dans A et +somme dans B, n'impacte pas les anciens transferts tant qu'il ne sont pas édités.
- correction affichage du compte dans l'ActionBar sur grand écrans (dont Galaxy Note 3)

Version 0.9.1 - 23/02/2014
- fix : des crashs en moins

Version 0.9 - 17/02/2014
- Nouveau : Pointage des opérations pour comparer avec le relevé
- Nouveau : Appuie long sur l'ajout d'opération rapide pour choisir la date de l'opération
- Nouveau : Les informations les plus utilisées sont en tête de proposition (désactivable dans les préfs)
- fix : n'affichait pas les opérations de plus de 3 mois 
- fix : corruption de la source et de la destination d'un transfert après édition de celui-ci
- fix : la somme n'était pas bonne après édition de la date d'une opération et en la mettant après la date de projection
- fix : lors du premier lancement, appuyer retour sur le formulaire de création de compte nous fait revenir dessus, en boucle

Version 0.8.8 - 08/10/2013
- fix de 3 crashes

Version 0.8.7 - 01/10/2013
- Sur le formulaire d'opération, les champs texte pouvaient se bloquer
- Modifier une somme positive d'opération existante la passait négative
- Mettre des donnée erronée dans la périodicité personnalisée pouvait crasher l'appli
- Le formulaire de compte accepte "+" en début de somme de départ désormais

Version 0.8.6 - 22/09/2013
- des catégories et des modes de paiement vides se créaient à chaque nouvelle opération

Version 0.8.5 - 21/09/2013
- La somme à la selection était incorrecte après un aller retour entre les comptes

Version 0.8.4 - 20/09/2013
- La somme à la selection était incorrecte

Version 0.8.3 - 18/09/2013
- correction de deux crashes rares

Version 0.8.2 - 01/09/2013
- fix un crash survivant de la 0.8.1
- fix un autre crash quand la ROM renvoie une locale non ISO (merci Kobo Arc)
- fix impossible de sélectionner une opération après suppression d'une opération

Version 0.8.1 - 28/08/2013
- fix de deux crashes

Version 0.8 - 26/08/2013
- refonte totale de l'interface
- nouvelle icône (par Kénil Quach)
- nettoyage des doublons de tiers/catégories/mode de paiement

Version 0.7.2 - 21/01/2013
- crash fix dans la tâche de fond

Version 0.7.1 - 21/01/2013
- 3 crash fix

Version 0.7 - 04/01/2013
- Transfert entre comptes
- Correction de crash (réécriture des accès base)

Version 0.6.16 - 10/06/2012
- Correction de crash
- Bug de l'annulation d'une modification de date d'une échéance qui ne se fait pas
- Bug de la complétion de l'ajout rapide sur la liste des comptes qui ne fonctionne pas
- Quand on crée une échéance alors qu'on viens de la liste d'opération, l'échéance est initialisée au compte courant

Version 0.6.15 - 23/03/2012
- fixe lorsque la devise n'as pas été correctement entrée
- affichage de l'année dans les échéances
- le format de l'année est tronqué à 2 caractères
- le format de la date est traduit (mois/jour pour l'anglais)
- nouvelle diminution de la taille de police sur les listes
- supprimer un compte supprime les échéances associées
- supprimer une occurrence propose du supprimer l'échéance, ou les suivantes seulement
- option pour masquer l'ajout rapide
- un crash fix

Version 0.6.14 - 04/03/2012
- fixe insertions des échéances jusqu'à la fin de l'année
- fixe affichage de la liste d'opération en double
- fixe de 6 crashes divers

Version 0.6.13 - 03/01/2012
- fixe listing des opérations de l'année d'avant quand janvier n'a aucune opération

Version 0.6.12 - 01/01/2012
- fixe listing des opérations de l'année d'avant

Version 0.6.11 - 28/11/2011
- fixe un autre bug d'insertion

Version 0.6.10 - 27/11/2011
- fixe un bug critique sur insertion infinie rendant Radis totalement inutilable, le nettoyage de la base peut prendre un peu de temps, si ça ne marche pas, désinstaller et réinstaller puis restaurer la dernière sauvagarde, désolé :(
- devise personnalisée
- en-tête de section de liste d'opération

Version 0.6.9 - 02/11/2011
- Nouvelle action avancée dans les comptes : recalculer les sommes
- La configuration est mise dans la base de donnée pour être sauvée lors de la sauvegarde des comptes
- Correction de somme inexacte après suppression d'opération
- Plusieurs crash fixes
- Correction dans la date de projection qui ne passait pas au mois suivant

Version 0.6.8 - 01/09/2011
- crash fix

Version 0.6.7 - 26/08/2011
- 3 crash fix

Version 0.6.6 - 24/08/2011
- 6 crash fix

Version 0.6.4/5 - 06/08/2011
- crash fix

Version 0.6.3 - 06/08/2011
- récupération des 2 derniers mois au lancement
- réglages divers sur le centrage sur l'opération sélectionnée, à la suppression et à l'ajout
- correction d'un crash pour les gens arrivant de 0.5.3 (conversion de base foireuse)
- correction d'un crash pour les échéances sur un compte inexistant
 
Version 0.6.2 - 01/08/2011
- centre la liste des opération sur la sélection
- le calcul de la date d'insertion était mal calculée lorsqu'on est le dernier jour du mois
- correction dans la récupération du mois suivant quand on est le dernier jour du mois

Version 0.6.1 - 29/07/2011
- correction de crash quand la date de projection est vide
- configuration de proguard pour avoir de meilleur rapports de crash

Version 0.6 - 28/07/2011
- Date de projection : choisissez la date de projection !
- Ajout rapide d'opération depuis la liste de comptes !
- correction de crash quand des champs sont vides ou erronés dans les échéances
- correction de création de catégories en double (si vous êtes touchés va falloir faire le ménage à la main, désolé :( )

Version 0.5.5 - 07/06/2011
- correction de création de catégories en double
- correction de somme erronée après édition d'une échéance et mise à jour des occurrences
- correction de crash si on a manqué la version 0.4 et mis à jour vers la 0.5
- correction d'autres bugs rare
- les accents ne sont plus pris en compte dans la complétion ("et" fait apparaître "éternel" par exemple)
- mettre un "+" dans une somme empêche l'insertion automatique du "-"
- robotium est utilisé pour des tests automatiques, afin d'améliorer la qualité

Version 0.5.4 - 22/05/2011
- correction crash quand le tiers est vide (le tiers est obligatoire désormais)
- contournement problème de popup de complétion en double sur Galaxy S

Version 0.5.3 - 20/05/2011
- récupération des opérations par mois au lieu des paquets de 25 avant
- correction de problème d'arrondis dans les sommes dont les sommes provoquait un solde incorrecte
- correction de 2 crash dans le service d'insertion
- correction dans l'algorithme d'insertion des échéances
- correction dans la programmation des échéances

Version 0.5.2 - 30/04/2011
- Attention, les préférences sont remise à zéro suite à un bug !
- résolution bug sur insertion
- résolution bug sur la sauvegarde des préférences sur Galaxy S
- périodicité mensuelle par défaut
- un click sur une échéance mène à l'éditeur
- ajout du lancement manuel des échéances (en cas de bug d'insertions automatique…)

Version 0.5.1 - 18/04/2011
- Correction d'un crash rare

Version 0.5 - 10/04/2011
- Échéancier : programmez des opérations périodiques !
- Débug de crash

Version 0.4.1 - 21/02/2011
- correction d'un crash lorsque radis tourne depuis un moment sur l'édition d'opération en tâche de fond et qu'on le ramène au premier plan

Version 0.4 - 20/02/2011
- révision de la charte graphique
- ajout rapide d'opération : pour noter une opération à la date courante
- révision des formulaires de saisie de compte et d’opération
- champs remarques pour les opérations
- date de la projection sur la liste des comptes
- ajout d’un envoi de rapport de plantage (lib ACRA)
- ajout de glissé droite-gauche et gauche-droite sur les listes de comptes et d'opérations, essayez les !
- correction de bugs et de crash

Version 0.3 - 15/02/2011
- corrige un gros bug quand on édite une opération et qu'on tourne l'écran : la somme du compte devient corrompue

Version 0.2 - 15/02/2011
 - Version anglaise
 - Sauvegarde/Restauration de la base de données sur carte SD
 - Message pour dire qu'il n'y a plus d'opération précédente
  
Version 0.1 - 11/02/2011
 - Première version publique
 - création/suppression/modification de comptes
 - création/suppression/modification d'opération
 - création/suppression/modification des tiers/catégories/modes de paiement
 - liste des opérations
 - soldes du compte à l'opération sélectionnée
 - auto sélection de l'opération au solde du jour
 - aide à la saisie
 - projection à l'opération la plus lointaine saisie
