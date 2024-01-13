package codes.blitz.game.bot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import codes.blitz.game.message.game.*;
import codes.blitz.game.message.game.actions.*;

public class Bot {
    public Bot() {
        System.out.println("Initializing your super duper mega bot.");
    }

    public List<Action> getActions(GameMessage gameMessage) {
        List<Action> actions = new ArrayList<>();

        // Vérifier si c'est le premier tick
        if (gameMessage.currentTickNumber() == 1) {
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
        }

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

        // Actions pour les membres d'équipage aux stations de tir (tourelles)
        List<TurretStation> operatedTurretStations = new ArrayList<>(myShip.stations().turrets());
        operatedTurretStations.removeIf(turretStation -> turretStation.operator() == null);

        for (TurretStation turretStation : operatedTurretStations) {
            actions.add(new TurretShootAction(turretStation.id()));
        }

        // Ajoutez ici d'autres actions répétitives pour chaque tick
        // Par exemple, réorienter le vaisseau, scanner avec le radar, etc.
        // ...

        return actions;
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
}