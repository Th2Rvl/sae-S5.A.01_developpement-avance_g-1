// Carte Leaflet de la tournée (maquette 04) : atelier, arrêts numérotés dans l'ordre de passage, tracé de la boucle,
// boutons zoom et « ma position » à droite. Données fournies par la page dans window.donneesCarte ;
// donnees.focus (identifiant d'un client) centre la carte sur ce client (lien « Voir sur la carte »).
(function () {
    var donnees = window.donneesCarte;
    var element = document.getElementById('carte');
    if (!donnees || !element || typeof L === 'undefined') {
        return;
    }
    var camion = '<svg viewBox="0 0 24 24"><path d="M20 8h-3V4H3c-1.1 0-2 .9-2 2v11h2c0 1.66 1.34 3 3 3s3-1.34 3-3h6c0 1.66 1.34 3 3 3s3-1.34 3-3h2v-5l-3-4zM6 18.5c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zm13.5-9l1.96 2.5H17V9.5h2.5zm-1.5 9c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5z"/></svg>';
    var cible = '<svg viewBox="0 0 24 24"><path d="M12 8c-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4-1.79-4-4-4zm8.94 3c-.46-4.17-3.77-7.48-7.94-7.94V1h-2v2.06C6.83 3.52 3.52 6.83 3.06 11H1v2h2.06c.46 4.17 3.77 7.48 7.94 7.94V23h2v-2.06c4.17-.46 7.48-3.77 7.94-7.94H23v-2h-2.06zM12 19c-3.87 0-7-3.13-7-7s3.13-7 7-7 7 3.13 7 7-3.13 7-7 7z"/></svg>';

    var carte = L.map(element, {zoomControl: false});
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19, attribution: '© contributeurs OpenStreetMap'
    }).addTo(carte);
    L.control.zoom({position: 'topright', zoomInTitle: 'Zoom avant', zoomOutTitle: 'Zoom arrière'}).addTo(carte);

    var Localiser = L.Control.extend({
        options: {position: 'topright'},
        onAdd: function () {
            var conteneur = L.DomUtil.create('div', 'leaflet-bar');
            var bouton = L.DomUtil.create('a', 'localiser', conteneur);
            bouton.href = '#';
            bouton.title = 'Ma position';
            bouton.setAttribute('role', 'button');
            bouton.innerHTML = cible;
            L.DomEvent.on(bouton, 'click', function (e) {
                L.DomEvent.preventDefault(e);
                carte.locate({setView: true, maxZoom: 15});
            });
            return conteneur;
        }
    });
    carte.addControl(new Localiser());
    carte.on('locationfound', function (e) {
        L.circleMarker(e.latlng, {radius: 7, color: '#fff', weight: 2, fillColor: '#1E5AA8', fillOpacity: 1}).addTo(carte);
    });

    var depot = [donnees.depot.lat, donnees.depot.lon];
    var points = [depot];
    L.marker(depot, {icon: L.divIcon({className: '', html: '<div class="marqueur-depot">' + camion + '</div>', iconSize: [28, 28]})})
        .addTo(carte).bindPopup('<strong>' + donnees.depot.nom + '</strong><br>Départ et retour');

    var focus = null;
    donnees.arrets.forEach(function (a) {
        var position = [a.lat, a.lon];
        points.push(position);
        var libelle = a.ordre > 0 ? a.ordre : '•';
        var marqueur = L.marker(position, {icon: L.divIcon({className: '',
            html: '<div class="marqueur-arret">' + libelle + '</div>', iconSize: [28, 28]})})
            .addTo(carte).bindPopup('<strong>' + a.nom + '</strong><br>' + a.ville);
        if (donnees.focus !== null && a.client === donnees.focus) {
            focus = {position: position, marqueur: marqueur};
        }
    });

    if (donnees.boucle) {
        L.polyline(points.concat([depot]), {color: '#1E3A5F', weight: 4, opacity: .85}).addTo(carte);
    }
    if (focus) {
        carte.setView(focus.position, 14);
        focus.marqueur.openPopup();
    } else {
        carte.fitBounds(L.latLngBounds(points).pad(0.15));
    }
})();
