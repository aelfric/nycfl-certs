(function (slides) {
    let currSlide = 0;

    let startTime = null;
    let isRunning = false;

    function loop(timeStamp) {
        if (!startTime) {
            startTime = timeStamp;
        }

        const timeDiff = timeStamp - startTime;
        if (timeDiff > 10000) {
            startTime = timeStamp;
            next();
        }
        if (isRunning) {
            requestAnimationFrame(loop);
        }
    }

    function switchSlide() {
        console.log({currSlide});
        for (let i = 0; i < slides.length; i++) {
            if (i === currSlide) {
                slides[i].classList.add("active");
            } else {
                slides[i].classList.remove("active");
            }
        }
    }

    function next() {
        currSlide = (currSlide + 1) % slides.length;
        switchSlide();
    }

    document.addEventListener('keydown', function (event) {
        if (event.key === 'k' || event.key === 'n') {
            next();
        } else if (event.key === 'r') {
            currSlide = 0;
            startTime = null;
            switchSlide();
        } else if (event.key === 's') {
            if (!isRunning) {
                isRunning = true;
                requestAnimationFrame(loop);
            }
        }
    });

    switchSlide();
    requestAnimationFrame(() => {
        document.body.classList.add("animated");
    });
})(document.querySelectorAll("section"));
