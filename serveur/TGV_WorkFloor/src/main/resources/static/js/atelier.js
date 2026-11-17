// Demande confirmation avant l'envoi d'un formulaire portant data-confirmation="Message".
document.addEventListener('submit', function (evenement) {
    var message = evenement.target.getAttribute('data-confirmation');
    if (message && !window.confirm(message)) {
        evenement.preventDefault();
    }
});
