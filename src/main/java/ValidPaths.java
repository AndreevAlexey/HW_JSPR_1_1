public enum ValidPaths {
    CLASSIC_HTML,
    INDEX_HTML,
    SPRING_SVG,
    SPRING_PNG,
    RESOURCES_HTML,
    STYLES_CSS,
    APP_JS,
    LINKS_HTML,
    FORMS_HTML,
    EVENTS_HTML,
    EVANTS_JS;

    @Override
    public String toString() {
        return "/" +
                this.name()
                .toLowerCase()
                .replace('_', '.');
    }

    // проверить наличие пути в списке возможных
    public static boolean checkPath(String path) {
        String str = path.replace("/","").replace('.', '_').toUpperCase();
        boolean result = true;
        try {
            ValidPaths.valueOf(str);
        } catch (IllegalArgumentException e) {
            result = false;
        }
        return result;
    }

    // получить ссылку на путь из списка возможных
    public static ValidPaths getValueByPath(String path) {
        String str = path.replace("/","").replace('.', '_').toUpperCase();
        try {
            return ValidPaths.valueOf(str);
        } catch (IllegalArgumentException e) {
           return null;
        }
    }
}
