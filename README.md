# Статистика успеваемости студентов по курсам на ulearn.me
Данное приложение представляет собой графическое отображение статистки успеваемости студентов по курсам с сайта ulearn.me.

В качестве входных данный должен поступать CSV файл выгрузки успеваемости с курса. Полученные данные парсятся и записываются в БД SQLite; файл базы данных создается в той же директории, где находится CSV файл. Все дальнейшие манипуляции по построению графиков происходят через созданную базу данных.  

В папке `data/` присутсвуют демонстрационные таблицы выгрузки успеваемости студентов по нескольким курсам. Опробуйте приложение на них. Для повторного создания базы данных, удалите соответсвующий файл `.db`, и приложение само сделает новый.

![Экран приложения](https://github.com/user-attachments/assets/0538735f-a39f-4076-8405-5e6ab1f60d44)

## Функционал приложения

- Сортировка статистки по разным типам баллов (отдельно баллы за упражнения, практики, вопросы и др.)
- Просмотр статистики успеваемости по различным критериям (общая, относительно пола, группы и т.д.)
- Просмотр успеваемости отдельно по модулям.
- Возможность фильтрации визулизируемых данных.
  - Фильтрация по количеству (count) - будут отображаться только графики, где данных не меньше указанного числа.
  - Фильтрация по имени (student) - будет отображаться график успеваемости конкретного студента.
- Возможность ображения баллов относительно максимума за задание (насколько процентов студент справился с каждым заданием).

![изображение](https://github.com/user-attachments/assets/e6722a29-0bd1-4d4d-8783-99c458ceff48)
![изображение](https://github.com/user-attachments/assets/35e54f97-2049-472b-8fed-7d94f0c55348)
![изображение](https://github.com/user-attachments/assets/a416596d-e8ff-4629-9bdc-bdb42db68853)

## Используемые технологии

- JavaFX - в качестве основы для GUI и построения графиков.
- SQLite - для создания и работы с базой данных.
- VK API Java SDK - для получения дополнительных данных о студентах.

Также для данного приложения был написан парсер для CSV таблиц выгрузки успеваемости с ulearn.me. 

## Запуск приложения

1. Из вкладки Releases скачайте JAR-файл последней версии приложения.
2. Поместите файл в нужную вам папку.
3. Запустите скачанный JAR-файл.

## Системные требования

- Любая ОС, для которой у вас есть виртуальная машина Java.

## Задачи

- Подготовка данных
  - [x] Модели данных (domain и data слои)
    - [ ] Удалить domain модели за фактической ненадобностью? 
  - [x] CSV-парсер для таблиц с ulearn.me
  - [x] Получение данных о студентах через VK Api.
    - [ ] Возникли странные проблемы с совместимостью VK API Java SDK с JavaFX. Починить и вернуть работу с vk api.
    - [ ] Добавить обработку капчи и таймаутов при работе с VK API.
    - [ ] Получить данные о странах студентов по их родным городам.
  - [x] Создание и работа с БД.
    - [ ] Исправить некорректную запись в некоторые неиспользуемые пока колонки (дата рождения).
    - [ ] Перейти на ORM модель работы с БД.

- Функционал:
	- [x] Общая статистика по успеваемости студентов (кнопка "Общее").
    - [x] Статистика по модулям.
	- [x] Статистика по критериям.
		- [x] Пол.
        - [x] Группа.
        - [x] Город.
        - [ ] Страна.
        - [ ] Еще критерии (возраст, университет...)?
	- [x] Статистика по категориям баллов.
      - [x] За упражнения.
      - [x] За практики.
      - [x] За контольные вопросы.
      - [ ] За активность.
      - [ ] За семинарские задачи.
	- [x] Статистика относительно максимального балла (% выполненности задания).
	- [x] Фильтрация данных.
      - [x] По количеству (отсеить те графики, у которых слишком мало студентов) 
      - [x] По имени студента (строит график успеваемости студента. Если студентов по имени несколько, берется средне значение по ним).
        - [ ] Добавить возможность фильтрации по фамилии.
	- [ ] Кнопка для дополнения данных студентов (обращение к VK API).
    - [ ] Кнопка открытия нового файла.
    - [ ] Добавить подсказки и подписи.
