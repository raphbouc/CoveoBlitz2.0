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
        idleCrewmates.removeIf(crewmate -> crewmate.currentStation() != null || crewmate.destination() != null);

        // Assigner les membres d'équipage aux stations de boucliers
        List<ShieldStation> shieldStations = new ArrayList<>(myShip.stations().shields());
        for (int i = 0; i < Math.min(2, idleCrewmates.size()); i++) {
            Crewmate crewmate = idleCrewmates.get(i);
            if (i < shieldStations.size()) {
                ShieldStation shieldStation = shieldStations.get(i);
                actions.add(new MoveCrewAction(crewmate.id(), shieldStation.gridPosition()));
                idleCrewmates.remove(crewmate);
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
            // Exemple de logique de tir
            actions.add(new TurretShootAction(turretStation.id()));
        }

        // Ajoutez ici d'autres actions répétitives pour chaque tick
        // Par exemple, réorienter le vaisseau, scanner avec le radar, etc.
        // ...

        return actions;
    }
}