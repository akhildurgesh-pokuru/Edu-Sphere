import { z } from 'zod';

const password = z
  .string()
  .min(8, 'Use at least 8 characters')
  .max(72, 'Use at most 72 characters')
  .regex(/[A-Za-z]/, 'Include at least one letter')
  .regex(/\d/, 'Include at least one number');

export const loginSchema = z.object({
  email: z.string().trim().min(1, 'Email is required').email('Enter a valid email'),
  password: z.string().min(1, 'Password is required'),
});

export const registerSchema = z
  .object({
    name: z.string().trim().min(1, 'Name is required').max(120, 'Name is too long'),
    email: z.string().trim().min(1, 'Email is required').email('Enter a valid email'),
    password,
    confirmPassword: z.string().min(1, 'Confirm your password'),
    role: z.enum(['STUDENT', 'TEACHER']),
  })
  .refine((d) => d.password === d.confirmPassword, { path: ['confirmPassword'], message: 'Passwords do not match' });

export const forgotSchema = z.object({ email: z.string().trim().min(1, 'Email is required').email('Enter a valid email') });

export const resetSchema = z
  .object({ password, confirmPassword: z.string().min(1, 'Confirm your password') })
  .refine((d) => d.password === d.confirmPassword, { path: ['confirmPassword'], message: 'Passwords do not match' });

export const profileSchema = z.object({
  name: z.string().trim().min(1, 'Name is required').max(120, 'Name is too long'),
  emailNotifications: z.boolean().optional(),
});

export const subjectSchema = z.object({
  title: z.string().trim().min(1, 'Title is required').max(200, 'Title is too long'),
  description: z.string().max(5000, 'Description is too long').optional().or(z.literal('')),
  category: z.string().max(80, 'Category is too long').optional().or(z.literal('')),
});

export const moduleSchema = z.object({
  title: z.string().trim().min(1, 'Title is required').max(200, 'Title is too long'),
  description: z.string().max(2000).optional().or(z.literal('')),
});

export const lessonSchema = z.object({
  title: z.string().trim().min(1, 'Title is required').max(200, 'Title is too long'),
  type: z.enum(['VIDEO', 'TEXT', 'DOCUMENT', 'LINK']),
  contentText: z.string().max(100000).optional().or(z.literal('')),
  externalUrl: z.string().max(500).optional().or(z.literal('')),
  durationMinutes: z.coerce.number().min(0).max(720).optional(),
  published: z.boolean().optional(),
});

export const quizSchema = z.object({
  title: z.string().trim().min(1, 'Title is required').max(200, 'Title is too long'),
  description: z.string().max(5000).optional().or(z.literal('')),
  durationMinutes: z.coerce.number({ invalid_type_error: 'Enter a number' }).int().min(1, 'At least 1 minute').max(600, 'At most 600 minutes'),
  maxAttempts: z.coerce.number({ invalid_type_error: 'Enter a number' }).int().min(1, 'At least 1').max(20, 'At most 20'),
  passingPercentage: z.coerce.number({ invalid_type_error: 'Enter a number' }).int().min(0, '0-100').max(100, '0-100'),
  startAt: z.string().optional().or(z.literal('')),
  endAt: z.string().optional().or(z.literal('')),
  showResults: z.boolean().optional(),
  randomize: z.boolean().optional(),
});

export const assignmentSchema = z.object({
  title: z.string().trim().min(1, 'Title is required').max(200, 'Title is too long'),
  description: z.string().max(5000).optional().or(z.literal('')),
  instructions: z.string().max(10000).optional().or(z.literal('')),
  dueAt: z.string().min(1, 'Due date is required'),
  maxMarks: z.coerce.number({ invalid_type_error: 'Enter a number' }).min(1, 'At least 1').max(1000, 'At most 1000'),
});

export const threadSchema = z.object({
  title: z.string().trim().min(1, 'Title is required').max(200, 'Title is too long'),
  body: z.string().trim().min(1, 'Message is required').max(10000, 'Message is too long'),
});

export const announcementSchema = z.object({
  title: z.string().trim().min(1, 'Title is required').max(200, 'Title is too long'),
  body: z.string().trim().min(1, 'Message is required').max(10000, 'Message is too long'),
  pinned: z.boolean().optional(),
});

export const CATEGORIES = ['Programming', 'Data Science', 'Design', 'Business', 'Mathematics', 'Science', 'Languages', 'Other'];
