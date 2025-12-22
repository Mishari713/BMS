window.onload = function() {
    const getCookie = (name) => {
        const value = `; ${document.cookie}`;
        const parts = value.split(`; ${name}=`);
        if (parts.length === 2) return parts.pop().split(';').shift();
    };

    const jwt = getCookie('JWT');
    if (jwt) {
        window.ui = SwaggerUIBundle({
            url: "/v3/api-docs",
            dom_id: '#swagger-ui',
            presets: [
                SwaggerUIBundle.presets.apis,
                SwaggerUIBundle.SwaggerUIStandalonePreset
            ],
            requestInterceptor: (req) => {
                req.headers['Authorization'] = 'Bearer ${jwt}';
                return req;
            }
        });
    }
};
