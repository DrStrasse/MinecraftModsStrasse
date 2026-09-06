# Mixin

Mixin (SpongePowered) — механизм байткод-инъекций в классы Minecraft. На Fabric это
основной способ менять ваниль; на NeoForge — крайняя мера, когда не хватает событий
и hook'ов API.

## 1. Конфиг

`src/main/resources/strassemods.mixins.json`:

```json
{
  "required": true,
  "package": "by.strasse.strassemods.mixin",
  "compatibilityLevel": "JAVA_21",
  "mixins": ["StrasseModsMixin"],
  "client": ["StrasseModsClientMixin"],
  "injectors": { "defaultRequire": 1 },
  "overwrites": { "requireAnnotations": true }
}
```

Подключение:

* **Fabric** — массив `"mixins"` в `fabric.mod.json`;
* **NeoForge** — блок в `neoforge.mods.toml`:

```toml
[[mixins]]
config="strassemods.mixins.json"
```

`compatibilityLevel` должен соответствовать целевой Java (`JAVA_17`/`JAVA_21`/`JAVA_25`).

## 2. Базовые инъекции

```java
@Mixin(MinecraftServer.class)
public class StrasseModsMixin {
    @Inject(at = @At("HEAD"), method = "loadLevel")
    private void strassemods$onLoadLevel(CallbackInfo ci) {
        // код в начале MinecraftServer.loadLevel()V
    }
}
```

| Аннотация | Когда применять |
|---|---|
| `@Inject` | добавить код в начало/конец/по месту; можно прервать через `CallbackInfo#cancel` |
| `@ModifyVariable` | подменить значение локальной переменной |
| `@ModifyArg` / `@ModifyArgs` | подменить аргумент вызова внутри метода |
| `@Redirect` | заменить вызов метода целиком (конфликтный — используйте осторожно) |
| `@WrapOperation` (MixinExtras) | «обернуть» вызов; совместимее, чем `@Redirect` |
| `@ModifyReturnValue` (MixinExtras) | изменить возвращаемое значение |
| `@Overwrite` | полная замена метода — только если иначе никак |
| `@Accessor` / `@Invoker` | доступ к приватным полям и методам |
| `@Shadow` | объявить существующее поле/метод класса-цели |

MixinExtras входит в поставку и Fabric Loader, и NeoForge — предпочитайте её
аннотации вместо `@Redirect`: они не «захватывают» точку целиком и лучше уживаются
с другими модами.

## 3. Правила хорошего тона

1. **Префиксуйте имена**: `strassemods$onLoadLevel`. Иначе два мода с методом
   `onLoadLevel` в одном классе-цели сломают друг друга.
2. `private` для всех методов-инъекций.
3. Не мешайте клиентские миксины в общий конфиг — держите отдельный
   `*.client.mixins.json` с `"environment": "client"` (Fabric) или разделением по
   `"client"`-массиву.
4. Минимизируйте площадь: лучше три точечные `@Inject`, чем один `@Overwrite`.
5. Каждая инъекция должна что-то найти: `defaultRequire: 1` заставит игру упасть на
   старте, если цель исчезла при обновлении версии, — это лучше молчаливой поломки.
6. Для доступа к приватным полям на NeoForge часто проще Access Transformer
   (`META-INF/accesstransformer.cfg`), чем `@Accessor`.

## 4. Целевые дескрипторы и маппинги

До 1.21.11 игра обфусцирована, поэтому Mixin использует **refmap** — таблицу
соответствия «имя в коде → intermediary/обфусцированное имя». Она генерируется
автоматически (Loom / ModDevGradle). Если рефмап не сгенерировался, инъекции
не находят цели в проде, хотя в dev всё работает.

С 26.1 игра деобфусцирована, refmap для ванильных целей больше не нужен —
имена совпадают везде.

При перегруженных методах указывайте полный дескриптор:

```java
@Inject(method = "damage(Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
```

## 5. Отладка

```bash
# показать, во что превратились классы после миксинов
./gradlew runClient -Dmixin.debug.export=true
# файлы появятся в run/.mixin.out/class/
```

Полезные системные свойства: `-Dmixin.debug=true`, `-Dmixin.debug.verbose=true`,
`-Dmixin.dumpTargetOnFailure=true`.

Типичные ошибки:

| Симптом | Причина |
|---|---|
| `Mixin apply failed ... target method not found` | сигнатура изменилась в новой версии MC |
| `CriticalInjectionException` | цель не найдена, а `require` > 0 |
| Работает в dev, падает в проде | не сгенерирован refmap / неверные маппинги |
| Конфликт с другим модом | `@Redirect` на популярной цели — переписать на `@WrapOperation` |

## 6. Ссылки

* Официальная документация Mixin: <https://github.com/SpongePowered/Mixin/wiki>
* Fabric Mixin-гайд: <https://docs.fabricmc.net/develop/getting-started/mixins>
* MixinExtras: <https://github.com/LlamaLad7/MixinExtras/wiki>
* NeoForge про миксины: <https://docs.neoforged.net/docs/advanced/accesstransformers>
