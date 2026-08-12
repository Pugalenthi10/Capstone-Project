document.getElementById("loginForm").addEventListener("submit", function(event) {

    event.preventDefault();

    let email = document.getElementById("email").value;
    let password = document.getElementById("password").value;
    let message = document.getElementById("message");

    if (email === "" || password === "") {

        message.innerHTML = "Please enter email and password.";
        message.style.color = "red";

    } else {

        message.innerHTML = "Login successful!";
        message.style.color = "green";

        // Go to property listing page
        setTimeout(function() {
            window.location.href = "home.html";
        }, 1000);
    }

});