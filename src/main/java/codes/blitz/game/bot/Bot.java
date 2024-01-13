package codes.blitz.game.bot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.util.Random;

import codes.blitz.game.message.game.*;
import codes.blitz.game.message.game.actions.*;

public class Bot {
    public Bot() {
        System.out.println("Initializing your super duper mega bot.");
    }
    private String flexibleCrewmateId = null; // ID du membre d'équipage flexible
    private boolean isFlexibleCrewmateAtTurret = false; // Indique si le membre flexible est actuellement à une tourelle


    public List<Action> getActions(GameMessage gameMessage) {
        List<Action> actions = new ArrayList<>();

        // Vérifier si c'est le premier tick
        if (gameMessage.currentTickNumber() <= 1) {
            // Actions spécifiques pour le premier tick
            actions.addAll(handleFirstTick(gameMessage));
        } else {
            // Actions pour les ticks suivants
            actions.addAll(handleSubsequentTicks(gameMessage));
        }

        return actions;
    }

    private List<Action> handleFirstTick(GameMessage gameMessage) {
        List<Action> actions = new ArrayList<>();
        Ship myShip = gameMessage.ships().get(gameMessage.currentTeamId());

        List<Crewmate> idleCrewmates = new ArrayList<>(myShip.crew());
        //idleCrewmates.removeIf(crewmate -> crewmate.currentStation() != null || crewmate.destination() != null);

        // Assignez 2 Crewmates aux stations de bouclier si disponibles
        List<ShieldStation> shieldStations = new ArrayList<>(myShip.stations().shields());
        for (int i = 0; i < Math.min(2, idleCrewmates.size()) && !shieldStations.isEmpty(); i++) {
            Crewmate crewmate = idleCrewmates.get(i);
            ShieldStation shieldStation = shieldStations.get(i);
            actions.add(new MoveCrewAction(crewmate.id(), shieldStation.gridPosition()));
            idleCrewmates.remove(crewmate);
            if (i == 0) { // Prendre le premier membre d'équipage pour être flexible
                flexibleCrewmateId = crewmate.id();
            }
        }
        Position farthestEnemyPosition = findFarthestEnemyShipPosition(myShip.worldPosition(), gameMessage.shipsPositions());

       // Vector positionEnnemy = new Vector((farthestEnemyPosition.x()), farthestEnemyPosition.y());

        // Assignez les Crewmates aux stations de type FAST, EMP, NORMAL, CANNON, SNIPER dans cet ordre de priorité
        List<TurretStation> turretStations = new ArrayList<>(myShip.stations().turrets());
        for (String turretType : List.of("FAST", "EMP", "NORMAL", "CANNON", "SNIPER")) {
            for (Crewmate crewmate : idleCrewmates) {
                TurretStation turretStation = findTurretByType(turretStations, turretType);
                if (turretStation != null) {
                    actions.add(new MoveCrewAction(crewmate.id(), turretStation.gridPosition()));
                    idleCrewmates.remove(crewmate);
                    break; // Sortir de la boucle interne une fois qu'un Crewmate est assigné
                }
            }
        }

        List<TurretStation> turretOrientation = new ArrayList<>(myShip.stations().turrets());
        System.out.println("liste  :  " + turretOrientation);
        for (TurretStation turretStation : turretStations) {

                //Vector test = new Vector (1,0);
                //actions.add(new TurretLookAtAction(turretStation.id(), positionEnnemy));
                //actions.add(new TurretLookAtAction(turretStation.id(), farthestEnemyPosition.toVector()));

            System.out.println("|||||||||"+ turretStation.operator()+ " -" + turretStation.orientationDegrees());
                actions.add(new RotateTurretAction(turretStation.id(), 270));
                System.out.println("turrelOrientationdegree    :   " +   turretStation.orientationDegrees());

                // actions.add(new TurretShootAction(turretStation.id()));
               // System.out.println("shooter   :   " + positionEnnemy);
                System.out.println(" direction   : " + farthestEnemyPosition);
                System.out.println("ajjsj    :  " + gameMessage.shipsPositions());

        }

        // Assigner les membres d'équipage aux stations d'armes
        for (Crewmate crewmate : idleCrewmates) {
            List<StationDistance> turrets = new ArrayList<>(crewmate.distanceFromStations().turrets());
            if (!turrets.isEmpty()) {
                StationDistance turretStation = turrets.get(0); // Choisissez la première station de tourelle disponible
                actions.add(new MoveCrewAction(crewmate.id(), turretStation.stationPosition()));
                idleCrewmates.remove(crewmate);
            }
            // Sortir de la boucle si tous les membres d'équipage inactifs ont été assignés
            if (idleCrewmates.isEmpty()) {
                break;
            }
        }
        return actions;
    }

    private List<Action> handleSubsequentTicks(GameMessage gameMessage) {
        List<Action> actions = new ArrayList<>();
        Ship myShip = gameMessage.ships().get(gameMessage.currentTeamId());

        // Vérifier l'état du bouclier et déplacer le membre d'équipage flexible si nécessaire
        if ((myShip.currentShield() > 75 && flexibleCrewmateId != null && !isFlexibleCrewmateAtTurret) || myShip.currentHealth() < 75) {
            TurretStation turretStation = findAvailableTurretStation(myShip.stations().turrets());
            if (turretStation != null) {
                actions.add(new MoveCrewAction(flexibleCrewmateId, turretStation.gridPosition()));
                isFlexibleCrewmateAtTurret = true; // Le membre est maintenant à une tourelle
            }
        } else if (myShip.currentShield() <= 25 && flexibleCrewmateId != null) {
            ShieldStation shieldStation = findAvailableShieldStation(myShip.stations().shields());
            if (shieldStation != null) {
                actions.add(new MoveCrewAction(flexibleCrewmateId, shieldStation.gridPosition()));
                isFlexibleCrewmateAtTurret = false; // Le membre est maintenant au bouclier
            }
        }
        Position farthestEnemyPosition = findFarthestEnemyShipPosition(myShip.worldPosition(), gameMessage.shipsPositions());

        // Actions pour les membres d'équipage aux stations de tir (tourelles)
        List<TurretStation> operatedTurretStations = new ArrayList<>(myShip.stations().turrets());
        operatedTurretStations.removeIf(turretStation -> turretStation.operator() == null);


        for (TurretStation turretStation : operatedTurretStations) {

            actions.add(new TurretShootAction(turretStation.id()));
            actions.add(new TurretLookAtAction(turretStation.id(), farthestEnemyPosition.toVector()));
        }

        // Ajoutez ici d'autres actions répétitives pour chaque tick
        // Par exemple, réorienter le vaisseau, scanner avec le radar, etc.
        // ...

        return actions;
    }

    // Méthode pour trouver une station de tourelle disponible
    private TurretStation findAvailableTurretStation(List<TurretStation> turretStations) {
        for (TurretStation turretStation : turretStations) {
            if (turretStation.operator() == null) {
                return turretStation;
            }
        }
        return null;
    }

    // Méthode pour trouver une station de bouclier disponible
    private ShieldStation findAvailableShieldStation(List<ShieldStation> shieldStations) {
        for (ShieldStation shieldStation : shieldStations) {
            if (shieldStation.operator() == null) {
                return shieldStation;
            }
        }
        return null;
    }
    // Méthode utilitaire pour trouver une TurretStation par type
    private TurretStation findTurretByType(List<TurretStation> turretStations, String turretType) {
        for (TurretStation turretStation : turretStations) {
            if (turretStation.turretType().equals(TurretType.valueOf(turretType))) {
                turretStations.remove(turretStation);
                return turretStation;
            }
        }
        return null;
    }

    private double calculateDistance(Position p1, Position p2) {
        double deltaX = p2.x() - p1.x();
        double deltaY = p2.y() - p1.y();
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }
    private Position findFarthestEnemyShipPosition(Position myPosition, Map<String, Position> shipsPositions) {
        double maxDistance = 0;
        Position farthestEnemyPosition = null;

        for (Map.Entry<String, Position> entry : shipsPositions.entrySet()) {
            String enemyId = entry.getKey();
            Position enemyPosition = entry.getValue();

            double distance = calculateDistance(myPosition, enemyPosition);

            if (distance > maxDistance) {
                maxDistance = distance;
                farthestEnemyPosition = enemyPosition;
            }
        }

        return farthestEnemyPosition;
    }
}