<template>
  <div
    ref="root"
    class="dashboard-select"
    :class="{ 'is-open': isOpen, 'is-disabled': disabled }"
    @focusout="handleFocusout"
  >
    <label :id="labelId" :for="id" class="dashboard-select__label">{{ label }}</label>
    <button
      ref="trigger"
      :id="id"
      type="button"
      class="dashboard-select__trigger"
      role="combobox"
      aria-haspopup="listbox"
      :aria-labelledby="`${labelId} ${valueId}`"
      :aria-expanded="isOpen"
      :aria-controls="isOpen ? listboxId : undefined"
      :aria-activedescendant="isOpen && options.length ? optionId(activeIndex) : undefined"
      :disabled="disabled"
      @click="toggleMenu"
      @keydown="handleKeydown"
    >
      <span :id="valueId" class="dashboard-select__value">
        {{ selectedOption?.label ?? placeholder }}
      </span>
      <svg
        class="dashboard-select__chevron"
        :class="{ 'is-rotated': isOpen }"
        aria-hidden="true"
        viewBox="0 0 20 20"
        fill="none"
      >
        <path d="m5.5 7.5 4.5 4.5 4.5-4.5" />
      </svg>
    </button>

    <Transition name="dashboard-select-pop">
      <div
        v-if="isOpen"
        :id="listboxId"
        ref="listbox"
        class="dashboard-select__listbox"
        role="listbox"
        :aria-labelledby="labelId"
      >
        <div
          v-for="(option, index) in options"
          :id="optionId(index)"
          :key="`${typeof option.value}:${String(option.value)}`"
          class="dashboard-select__option"
          :class="{
            'is-selected': option.value === modelValue,
            'is-active': index === activeIndex,
          }"
          role="option"
          :aria-selected="option.value === modelValue"
          :data-index="index"
          @pointerdown.prevent
          @pointerenter="activeIndex = index"
          @click="selectOption(option)"
        >
          <span class="dashboard-select__option-label">{{ option.label }}</span>
          <svg
            v-if="option.value === modelValue"
            class="dashboard-select__check"
            aria-hidden="true"
            viewBox="0 0 20 20"
            fill="none"
          >
            <path d="m4.5 10.5 3.5 3.5 7.5-8" />
          </svg>
        </div>
        <p v-if="!options.length" class="dashboard-select__empty">No options available</p>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

export interface DashboardSelectOption {
  value: string | number | null
  label: string
}

const props = withDefaults(defineProps<{
  id: string
  label: string
  modelValue: DashboardSelectOption['value']
  options: DashboardSelectOption[]
  placeholder?: string
  disabled?: boolean
}>(), {
  placeholder: 'Choose an option',
  disabled: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: DashboardSelectOption['value']]
}>()

const root = ref<HTMLElement | null>(null)
const trigger = ref<HTMLButtonElement | null>(null)
const listbox = ref<HTMLElement | null>(null)
const isOpen = ref(false)
const activeIndex = ref(0)
const typeahead = ref('')
let typeaheadTimer: ReturnType<typeof setTimeout> | undefined

const labelId = computed(() => `${props.id}-label`)
const valueId = computed(() => `${props.id}-value`)
const listboxId = computed(() => `${props.id}-listbox`)
const selectedOption = computed(() => props.options.find((option) => option.value === props.modelValue))

const optionId = (index: number) => `${props.id}-option-${index}`

const scrollActiveOptionIntoView = () => {
  nextTick(() => {
    const activeOption = listbox.value?.querySelector<HTMLElement>(`[data-index="${activeIndex.value}"]`)
    activeOption?.scrollIntoView?.({ block: 'nearest' })
  })
}

const openMenu = (edge?: 'first' | 'last') => {
  if (props.disabled || !props.options.length) return
  const selectedIndex = props.options.findIndex((option) => option.value === props.modelValue)
  activeIndex.value = edge === 'first'
    ? 0
    : edge === 'last'
      ? props.options.length - 1
      : Math.max(0, selectedIndex)
  isOpen.value = true
  scrollActiveOptionIntoView()
}

const closeMenu = (restoreFocus = false) => {
  isOpen.value = false
  if (restoreFocus) nextTick(() => trigger.value?.focus())
}

const toggleMenu = () => {
  if (isOpen.value) closeMenu()
  else openMenu()
}

const selectOption = (option: DashboardSelectOption) => {
  emit('update:modelValue', option.value)
  closeMenu(true)
}

const moveActiveOption = (offset: number) => {
  if (!props.options.length) return
  activeIndex.value = (activeIndex.value + offset + props.options.length) % props.options.length
  scrollActiveOptionIntoView()
}

const handleTypeahead = (key: string) => {
  typeahead.value += key.toLocaleLowerCase()
  if (typeaheadTimer) clearTimeout(typeaheadTimer)
  typeaheadTimer = setTimeout(() => {
    typeahead.value = ''
  }, 600)

  const matchIndex = props.options.findIndex((option) =>
    option.label.toLocaleLowerCase().startsWith(typeahead.value),
  )
  if (matchIndex >= 0) {
    activeIndex.value = matchIndex
    if (!isOpen.value) isOpen.value = true
    scrollActiveOptionIntoView()
  }
}

const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape' && isOpen.value) {
    event.preventDefault()
    closeMenu(true)
    return
  }

  if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
    event.preventDefault()
    if (!isOpen.value) openMenu()
    else moveActiveOption(event.key === 'ArrowDown' ? 1 : -1)
    return
  }

  if (event.key === 'Home' || event.key === 'End') {
    event.preventDefault()
    if (!isOpen.value) openMenu(event.key === 'Home' ? 'first' : 'last')
    else {
      activeIndex.value = event.key === 'Home' ? 0 : props.options.length - 1
      scrollActiveOptionIntoView()
    }
    return
  }

  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    if (!isOpen.value) openMenu()
    else if (props.options[activeIndex.value]) selectOption(props.options[activeIndex.value])
    return
  }

  if (event.key.length === 1 && !event.altKey && !event.ctrlKey && !event.metaKey) {
    handleTypeahead(event.key)
  }
}

const handleOutsidePointer = (event: PointerEvent) => {
  if (root.value && event.target instanceof Node && !root.value.contains(event.target)) {
    closeMenu()
  }
}

const handleFocusout = (event: FocusEvent) => {
  if (
    root.value &&
    (!(event.relatedTarget instanceof Node) || !root.value.contains(event.relatedTarget))
  ) {
    closeMenu()
  }
}

watch(() => props.options, () => {
  if (activeIndex.value >= props.options.length) activeIndex.value = Math.max(0, props.options.length - 1)
  if (!props.options.length) closeMenu()
})

onMounted(() => document.addEventListener('pointerdown', handleOutsidePointer))
onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', handleOutsidePointer)
  if (typeaheadTimer) clearTimeout(typeaheadTimer)
})
</script>

<style scoped>
.dashboard-select {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
  min-width: 0;
}

.dashboard-select.is-open {
  z-index: 120;
}

.dashboard-select__label {
  color: #475569;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  line-height: 1.2;
  text-transform: uppercase;
}

.dashboard-select__trigger {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 16px;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-height: 44px;
  padding: 10px 14px;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  background: #ffffff;
  color: #0f172a;
  font: inherit;
  font-size: 0.95rem;
  font-weight: 500;
  text-align: left;
  cursor: pointer;
  transition: border-color 160ms ease, box-shadow 160ms ease, background-color 160ms ease;
}

.dashboard-select__trigger:hover:not(:disabled) {
  border-color: #94a3b8;
  background: #fffdfa;
}

.dashboard-select__trigger:focus-visible,
.dashboard-select.is-open .dashboard-select__trigger {
  border-color: #f97316;
  outline: none;
  box-shadow: 0 0 0 3px rgba(249, 115, 22, 0.14);
}

.dashboard-select__trigger:disabled {
  background: #f8fafc;
  color: #94a3b8;
  cursor: not-allowed;
}

.dashboard-select__value {
  min-width: 0;
  color: #0f172a;
  font-size: 0.93rem;
  font-weight: 500;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.dashboard-select__chevron {
  width: 16px;
  height: 16px;
  stroke: #64748b;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.8;
  transition: transform 160ms ease, stroke 160ms ease;
}

.dashboard-select__chevron.is-rotated {
  transform: rotate(180deg);
  stroke: #f97316;
}

.dashboard-select__listbox {
  position: absolute;
  inset: calc(100% + 7px) 0 auto;
  z-index: 121;
  max-height: min(19rem, 50vh);
  overflow: auto;
  padding: 6px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 14px 30px -8px rgba(15, 23, 42, 0.18), 0 4px 10px -5px rgba(15, 23, 42, 0.12);
  overscroll-behavior: contain;
  scrollbar-color: #cbd5e1 transparent;
  scrollbar-width: thin;
}

.dashboard-select__listbox::-webkit-scrollbar {
  width: 7px;
}

.dashboard-select__listbox::-webkit-scrollbar-thumb {
  border: 2px solid #ffffff;
  border-radius: 999px;
  background: #cbd5e1;
}

.dashboard-select__option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 42px;
  padding: 9px 11px;
  border-radius: 8px;
  color: #334155;
  font-size: 0.88rem;
  line-height: 1.35;
  cursor: pointer;
  transition: background-color 130ms ease, color 130ms ease;
}

.dashboard-select__option:hover,
.dashboard-select__option.is-active {
  background: #f8fafc;
  color: #0f172a;
}

.dashboard-select__option.is-selected {
  background: #fff7ed;
  color: #9a3412;
  font-weight: 600;
}

.dashboard-select__option.is-selected.is-active {
  background: #ffedd5;
}

.dashboard-select__option-label {
  min-width: 0;
  overflow-wrap: anywhere;
}

.dashboard-select__check {
  flex: 0 0 16px;
  width: 16px;
  height: 16px;
  stroke: #ea580c;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2.2;
}

.dashboard-select__empty {
  margin: 0;
  padding: 12px;
  color: #64748b;
  font-size: 0.86rem;
}

.dashboard-select-pop-enter-active,
.dashboard-select-pop-leave-active {
  transition: opacity 140ms ease, transform 140ms cubic-bezier(0.16, 1, 0.3, 1);
  transform-origin: top center;
}

.dashboard-select-pop-enter-from,
.dashboard-select-pop-leave-to {
  transform: translateY(-4px) scale(0.99);
  opacity: 0;
}

@media (prefers-reduced-motion: reduce) {
  .dashboard-select__trigger,
  .dashboard-select__chevron,
  .dashboard-select__option,
  .dashboard-select-pop-enter-active,
  .dashboard-select-pop-leave-active {
    transition-duration: 0.01ms;
  }
}
</style>
