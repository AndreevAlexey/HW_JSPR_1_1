public enum Methods {
    GET,
    POST;

    // получить ссылку на путь из списка возможных
    public static Methods getValueByName(String name) {
        try {
            return Methods.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
