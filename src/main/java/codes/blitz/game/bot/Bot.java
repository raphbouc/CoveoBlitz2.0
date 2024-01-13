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

        Ship myShip = gameMessage.ships().get(gameMessage.currentTeamId());
        List<String> otherShipsIds = new ArrayList<>(gameMessage.shipsPositions().keySet());
        otherShipsIds.removeIf(shipId -> shipId.equals(gameMessage.currentTeamId()));

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



        // Now assign other idle crewmates to random stations
        for (Crewmate crewmate : idleCrewmates) {
            List<StationDistance> visitableStations = new ArrayList<>();
            visitableStations.addAll(crewmate.distanceFromStations().turrets());

            if (!visitableStations.isEmpty()) {
                StationDistance stationToMoveTo = visitableStations.get(new Random().nextInt(visitableStations.size()));
                actions.add(new MoveCrewAction(crewmate.id(),
                stationToMoveTo.stationPosition()));
            }
        }
        // Now crew members at stations should do something!
        List<TurretStation> operatedTurretStations = new ArrayList<>(myShip.stations().turrets());
        operatedTurretStations.removeIf(turretStation -> turretStation.operator() == null);
        for (TurretStation turretStation : operatedTurretStations) {
            actions.add(new TurretShootAction(turretStation.id()));
        }

        List<HelmStation> operatedHelmStation = new ArrayList<>(myShip.stations().helms());
        operatedHelmStation.removeIf(helmStation -> helmStation.operator() == null);
        for (HelmStation helmStation : operatedHelmStation) {
            actions.add(new RotateShipAction(90));
        }

        List<RadarStation> operatedRadarStations = new ArrayList<>(myShip.stations().radars());
        operatedRadarStations.removeIf(radarStation -> radarStation.operator() == null);
        for (RadarStation radarStation : operatedRadarStations) {
            actions.add(new RadarScanAction(radarStation.id(), otherShipsIds.get(new Random().nextInt(otherShipsIds.size()))));
        }

        // You can clearly do better than the random actions above. Have fun!!
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