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
        idleCrewmates.removeIf(crewmate -> crewmate.currentStation() != null || crewmate.destination() != null);

        TurretStation fastTurret = null;
        List<TurretStation> allTurrets = myShip.stations().turrets();

        for (TurretStation turretStation : allTurrets) {
            // Assurez-vous que la comparaison avec le type de turret est correcte
            if (turretStation.turretType() == TurretType.FAST) {
                fastTurret = turretStation;
                break;
            }
        }

        if (fastTurret != null && !idleCrewmates.isEmpty()) {
            Crewmate crewmate = idleCrewmates.get(0);
            actions.add(new MoveCrewAction(crewmate.id(), fastTurret.gridPosition()));
            idleCrewmates.remove(crewmate);
        }


        List<ShieldStation> shieldStations = new ArrayList<>(myShip.stations().shields());
        for (int i = 0; i < Math.min(2, idleCrewmates.size()); i++) {
            Crewmate crewmate = idleCrewmates.get(i);
            if (i < shieldStations.size()) {
                ShieldStation shieldStation = shieldStations.get(i);
                actions.add(new MoveCrewAction(crewmate.id(), shieldStation.gridPosition()));
                idleCrewmates.remove(crewmate);
            }
        }

        List<HelmStation> helmStations = new ArrayList<>(myShip.stations().helms());
        for (int i = 0; i < Math.min(2, idleCrewmates.size()); i++) {
            Crewmate crewmate = idleCrewmates.get(i);
            if (i < helmStations.size()) {
                HelmStation helmStation = helmStations.get(i);
                actions.add(new MoveCrewAction(crewmate.id(), helmStation.gridPosition()));
                idleCrewmates.remove(crewmate);
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
            int switchAction = new Random().nextInt(3);
            switch (switchAction) {
                case 0:
                    // Charge the turret
                    actions.add(new TurretChargeAction(turretStation.id()));
                    break;
                case 1:
                    // Aim at the turret itself
                    actions.add(new TurretLookAtAction(turretStation.id(), new Vector(gameMessage.constants().world().width() * Math.random(), gameMessage.constants().world().width() * Math.random())));
                    break;
                case 2:
                    // Shoot!
                    actions.add(new TurretShootAction(turretStation.id()));
                    break;
            }
        }

        List<HelmStation> operatedHelmStation = new ArrayList<>(myShip.stations().helms());
        operatedHelmStation.removeIf(helmStation -> helmStation.operator() == null);
        for (HelmStation helmStation : operatedHelmStation) {
            // Look at the nearest enemy
            if (!otherShipsIds.isEmpty()) {
                String enemyId = otherShipsIds.get(0);
                Position enemyPosition = gameMessage.shipsPositions().get(enemyId);

                // Calculate the vector to the enemy
                Vector directionToEnemy = new Vector(enemyPosition.x() - myShip.worldPosition().x(), enemyPosition.y() - myShip.worldPosition().y());

                // Use LookAt action to face the enemy
                actions.add(new ShipLookAtAction(directionToEnemy));
            }
        }


        List<RadarStation> operatedRadarStations = new ArrayList<>(myShip.stations().radars());
        operatedRadarStations.removeIf(radarStation -> radarStation.operator() == null);
        for (RadarStation radarStation : operatedRadarStations) {
            actions.add(new RadarScanAction(radarStation.id(), otherShipsIds.get(new Random().nextInt(otherShipsIds.size()))));
        }

        // You can clearly do better than the random actions above. Have fun!!
        return actions;
    }
}