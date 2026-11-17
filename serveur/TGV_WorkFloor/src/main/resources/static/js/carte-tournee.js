// Carte Leaflet d'une tournée : dépôt (atelier), arrêts numérotés dans l'ordre de passage, tracé de la boucle.
// Les données sont fournies par la page dans window.donneesCarte ; la carte s'affiche dans l'élément #carte.
(function () {
    var donnees = window.donneesCarte;
    var element = document.getElementById('carte');
    if (!donnees || !element || typeof L === 'undefined') {
        return;
    }
    var camion = '<svg viewBox="0 0 24 24"><path d="M20 8h-3V4H3c-1.1 0-2 .9-2 2v11h2c0 1.66 1.34 3 3 3s3-1.34 3-3h6c0 1.66 1.34 3 3 3s3-1.34 3-3h2v-5l-3-4zM6 18.5c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zm13.5-9l1.96 2.5H17V9.5h2.5zm-1.5 9c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5z"/></svg>';
    var carte = L.map(element, {scrollWheelZoom: false, tap: true});
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19, attribution: '© contributeurs OpenStreetMap'
    }).addTo(carte);

    var depot = [donnees.depot.lat, donnees.depot.lon];
    var points = [depot];
    L.marker(depot, {icon: L.divIcon({className: '', html: '<div class="marqueur-depot">' + camion + '</div>', iconSize: [30, 30]})})
        .addTo(carte).bindPopup('<strong>' + donnees.depot.nom + '</strong><br>Départ et retour');

    donnees.arrets.forEach(function (a) {
        var position = [a.lat, a.lon];
        points.push(position);
        var libelle = a.ordre > 0 ? a.ordre : '•';
        L.marker(position, {icon: L.divIcon({className: '',
            html: '<div class="marqueur-arret' + (a.livre ? ' livre' : '') + '">' + libelle + '</div>', iconSize: [30, 30]})})
            .addTo(carte).bindPopup('<strong>' + a.nom + '</strong><br>' + a.ville);
    });

    if (donnees.boucle) {
        L.polyline(points.concat([depot]), {color: '#1E3A5F', weight: 4, opacity: .85}).addTo(carte);
    }
    carte.fitBounds(L.latLngBounds(points).pad(0.15));
})();
