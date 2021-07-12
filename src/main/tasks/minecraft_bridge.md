## Minecraft -> Discord bridge
### barebones

- [ ] Добавить внутриигровую команду `/link [тег example#0000]` и обрабатывать следующие ошибки:
- Если игрок неверно указал ник в дискорде, выдать ошибку
- Выдать ошибку, если игрок не указал ник
- Если у дискорд-аккаунта закрыта лс выдавать ошибку игроку
- [ ] Плагин должен высылать в лс человеку по указанному нику эмбед с 3 кнопками:
- *"Привязать" - заносит данные об игроке в соответствующие поля в таблице*
- *"Отмена" - отменяет привязку аккаунта*
- *"Спам" - запрещает присылать запросы о привязке данному дискорд аккаунту от данного игрока (сброс после перезапуска)*
- [ ] Пользователь не должен иметь возможность привязывать аккаунт, если он уже имеет привязку
- [ ] Пользователь не должен иметь возможность привязывать аккаунт к уже привязанному аккаунту
- [ ] Пользователь должен иметь возможность в любое время отвязать свой аккаунт
- [ ] Во избежание флуда, привязка аккаунта должна быть доступна 1 раз в 1 минуту для игрока
- [ ] Если у игрока есть права на привязку аккаунта - он может привязать, иначе - нет
### finish
- [ ] Настроить полную связь с конфигом. Все короткие строки должны настраиватся через конфиг
- [ ] Ввести форматтирование цветов в игре с помощью hex кодов
- [ ] Отправлять эмбеды вместо сообщений